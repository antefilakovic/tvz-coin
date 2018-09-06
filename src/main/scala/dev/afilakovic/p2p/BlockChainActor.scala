package dev.afilakovic.p2p

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive
import com.typesafe.scalalogging.Logger
import dev.afilakovic.blockchain.{Block, BlockChain, SignedTransaction, Transaction}
import dev.afilakovic.p2p.NetworkActor.{BlockChainUpdated, BroadcastRequest}

import scala.util.{Failure, Success}

object BlockChainActor {

  case class NewBlock(block: Block)

  case object GetLast

  case object GetAllBlocks

  case object GetBlockChain

  case class NewBlockChain(blocks: Seq[Block])

  case class CurrentBlockChain(blockChain: BlockChain)

  def props(peerToPeer: ActorRef) = Props(new BlockChainActor(BlockChain(), peerToPeer))
}

class BlockChainActor(var blockChain: BlockChain, network: ActorRef) extends Actor {

  import dev.afilakovic.p2p.BlockChainActor._

  val logger = Logger(classOf[BlockChainActor])

  override def receive: Receive = LoggingReceive {
    case NewBlock(block) => newBlock(block, sender())
    case NewBlockChain(blocks) => newBlockChain(blocks)
    case GetLast => sender() ! NewBlock(blockChain.lastBlock)
    case GetAllBlocks => sender() ! NewBlockChain(blockChain.list)
    case GetBlockChain => sender() ! CurrentBlockChain(blockChain)
  }

  def newBlock(block: Block, peer: ActorRef) = {
    val lastBlock = blockChain.lastBlock
    logger.info(s"New block with index <${block.index}> received while last index is <${lastBlock.index}>")

    if (block.index == lastBlock.index + 1) {
      blockChain.addBlock(block) match {
        case Success(newBlockChain) => logger.info("Received valid block, updating blockchain")
          blockChain = newBlockChain
          network ! BroadcastRequest(NewBlock(blockChain.lastBlock))
          network ! BlockChainUpdated(blockChain)
        case Failure(_) => logger.info("Adding new block failed, querying whole chain")
          peer ! GetAllBlocks
      }
    } else if (block.index <= lastBlock.index) {
      logger.debug("Block was not newer than last block, ignoring.")
    } else {
      logger.info("Block is more than 1 ahead, get the chain from our peer")
      peer ! GetAllBlocks
    }
  }

  def newBlockChain(blocks: Seq[Block]) = {
    val lastBlock = blockChain.lastBlock
    logger.info(s"${blocks.length} blocks received.")

    blocks match {
      case Nil => logger.warn("Received an empty block list, discarding")
      case _ :+ lastReceived if lastReceived.index <= lastBlock.index =>
        logger.debug("Received block chain is shorter than the current block chain. Do nothing")
      case _ =>
        logger.info("Received block chain is longer than the current block chain")
        BlockChain.createWith(blocks) match {
          case Success(newChain) =>
            blockChain = newChain
            network ! BroadcastRequest(NewBlock(blockChain.lastBlock))
            network ! BlockChainUpdated(blockChain)
          case Failure(e) => logger.error("Rejecting received chain.", e)
        }
    }
  }
}
