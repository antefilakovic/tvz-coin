package dev.afilakovic.p2p

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive
import com.typesafe.scalalogging.Logger
import dev.afilakovic.blockchain._
import dev.afilakovic.p2p.MinerActor.MineBlock
import dev.afilakovic.p2p.MiningMasterActor.MineResult

import scala.collection.immutable.Seq

object MinerActor {

  case class MineBlock(blockChain: BlockChain,
                       transactions: Seq[SignedTransaction],
                       nonce: Long = 0,
                       timeStamp: Long = System.currentTimeMillis)

  def props(master: ActorRef): Props = Props(new MinerActor(master))
}

class MinerActor(master: ActorRef) extends Actor {
  val logger = Logger(classOf[MinerActor])

  override def receive: Receive = LoggingReceive {
    case MineBlock(blockChain, transactions, nonce, timeStamp) =>
      var transactionsToMine = transactions
      if (nonce == 0) {
        logger.debug(s"Starting mining attempt for ${transactions.size} transactions with index ${blockChain.lastBlock.index + 1}")
        transactionsToMine = transactions :+ BlockReward.create
      }

      val lastBlock = blockChain.lastBlock
      val newBlock = Block(lastBlock.index + 1, lastBlock.hash, transactionsToMine.toSet, nonce, timeStamp)

      if (BlockChain.proofOfWork(newBlock)) {
        logger.debug(s"Found a block with index ${newBlock.index} after ${newBlock.nonce + 1} attempts.")
        master ! MineResult(newBlock)
        context.stop(self)
      } else {
        self ! MineBlock(blockChain, transactions, nonce + 1, timeStamp)
      }
  }

  override def postStop(): Unit = logger.debug("Aborted mining.")
}

