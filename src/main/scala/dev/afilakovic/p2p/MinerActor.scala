package dev.afilakovic.p2p

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive
import com.typesafe.scalalogging.Logger
import dev.afilakovic.blockchain._
import dev.afilakovic.p2p.MinerActor.MineBlock
import dev.afilakovic.p2p.MiningMasterActor.MineResult

object MinerActor {

  case class MineBlock(blockChain: BlockChain,
                       transactions: Set[SignedTransaction],
                       nonce: Long = 0,
                       timeStamp: Long = System.currentTimeMillis)

  def props(master: ActorRef): Props = Props(new MinerActor(master))
}

class MinerActor(master: ActorRef) extends Actor {
  val logger = Logger(classOf[MinerActor])

  override def receive: Receive = LoggingReceive {
    case MineBlock(blockChain, transactions, nonce, timeStamp) =>
      var transactionsToMine: Set[SignedTransaction] = transactions
      if (nonce == 0) {
        transactionsToMine = transactionsToMine ++ Set(BlockReward.create)
        logger.info(s"Starting mining attempt for ${transactionsToMine.size} transactions with index ${blockChain.lastBlock.index + 1}")
      }

      val lastBlock = blockChain.lastBlock
      val newBlock = Block(lastBlock.index + 1, lastBlock.hash, transactionsToMine, nonce, timeStamp)

      if (BlockChain.proofOfWork(newBlock)) {
        logger.info(s"Found a block with index ${newBlock.index} after ${newBlock.nonce + 1} attempts.")
        master ! MineResult(newBlock)
        context.stop(self)
      } else {
        self ! MineBlock(blockChain, transactionsToMine, nonce + 1, timeStamp)
      }
  }

  override def postStop(): Unit = logger.debug("Aborted mining.")
}

