package dev.afilakovic.p2p

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive
import com.typesafe.scalalogging.Logger
import dev.afilakovic.AppConfig
import dev.afilakovic.blockchain._
import dev.afilakovic.p2p.NetworkActor.{BlockChainUpdated, BroadcastRequest}

import scala.util.{Failure, Success, Try}

object BlockChainActor {

  case class NewBlock(block: Block)

  case object GetLast

  case object GetAllBlocks

  case object GetBlockChain

  case class NewBlockChain(blocks: Seq[Block])

  case class CurrentBlockChain(blockChain: BlockChain)

  case class CreateTransaction(amount: BigDecimal, payee: String)

  case class TransactionCreated(signedTx: Try[SignedTransaction])

  case object GetBalance

  case class Balance(balance: BigDecimal)

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
    case CreateTransaction(amount, payee) => createTransaction(amount, payee, sender())
    case GetBalance => sender() ! Balance(blockChain.unspentTransactionsForUser(AppConfig.SIGNATURE.address).map(_.amount).sum)
  }

  def createTransaction(amount: BigDecimal, payee: String, sender: ActorRef) = {
    logger.info(s"${blockChain.list.flatMap(_.signedTransaction).map(_.transaction).flatMap(_.output).map(_.payee).distinct.length} users mentioned in chain")
    val unspentTx = blockChain.unspentTransactionsForUser(AppConfig.SIGNATURE.address).toList
    var txToUse = Set.empty[TransactionOutput]
    var sum = BigDecimal(0)
    var i = 0
    while (sum < amount && i < unspentTx.size) {
      val tx = unspentTx(i)
      sum += tx.amount
      txToUse = txToUse ++ Set(tx)
      i = i + 1
    }

    sender ! BlockChainActor.TransactionCreated(TransactionCreator(txToUse.toSeq, amount, payee))
  }

  def newBlock(block: Block, peer: ActorRef) = {
    val lastBlock = blockChain.lastBlock
    logger.info(s"New block index <${block.index}> - last index <${lastBlock.index}>")

    if (block.index == lastBlock.index + 1) {
      blockChain.addBlock(block) match {
        case Success(newBlockChain) => logger.info("Valid block, updating chain")
          blockChain = newBlockChain
          network ! BroadcastRequest(NewBlock(blockChain.lastBlock))
          network ! BlockChainUpdated(blockChain)
        case Failure(_) => logger.info("Adding failed, getting whole chain")
          peer ! GetAllBlocks
      }
    } else if (block.index <= lastBlock.index) {
      logger.debug("Block not newer, ignoring.")
    } else {
      logger.info("Block more than 1 ahead, getting the chain from peer")
      peer ! GetAllBlocks
    }
  }

  def newBlockChain(blocks: Seq[Block]) = {
    val lastBlock = blockChain.lastBlock
    logger.info(s"${blocks.length} blocks received.")

    blocks match {
      case Nil => logger.warn("Empty block list, discarding")
      case _ :+ lastReceived if lastReceived.index <= lastBlock.index =>
        logger.debug("Blockchain is shorter than the current blockchain. Do nothing")
      case _ =>
        logger.info("Blockchain is longer than the current blockchain")
        BlockChain.createWith(blocks) match {
          case Success(newChain) =>
            blockChain = newChain
            network ! BroadcastRequest(NewBlock(blockChain.lastBlock))
            network ! BlockChainUpdated(blockChain)
          case Failure(e) => logger.error("Rejecting blockchain.", e)
        }
    }
  }
}
