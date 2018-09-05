package dev.afilakovic.p2p

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorRefFactory, PoisonPill, Props, Terminated}
import akka.event.LoggingReceive
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import dev.afilakovic.blockchain.{Block, BlockChain, Transaction}
import dev.afilakovic.p2p.BlockChainActor.{AddTransactions, CurrentBlockChain, GetBlockChain}
import dev.afilakovic.p2p.NetworkActor.BroadcastRequest

import scala.collection.immutable.{Seq, Set}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

object MiningMasterActor {

  case class MineBlock(blockChain: BlockChain, transactions: Seq[Transaction])

  case class BlockChainChanged(blockchain: BlockChain)

  case class MineResult(block: Block)

  def props(network: ActorRef)(implicit ec: ExecutionContext) = Props(new MiningMasterActor(network))
}

class MiningMasterActor(network: ActorRef)(implicit ec: ExecutionContext) extends Actor {

  import dev.afilakovic.p2p.MiningMasterActor._

  val logger = Logger(classOf[MiningMasterActor])

  implicit val timeout = Timeout(Duration(5, TimeUnit.SECONDS))

  var miners: Set[ActorRef] = Set.empty
  var transactions: Set[Transaction] = Set.empty

  def createWorker(factory: ActorRefFactory): ActorRef = factory.actorOf(MinerActor.props(self))

  override def receive = LoggingReceive {
    case MineBlock(blockChain, requestTransactions) =>
      logger.debug(s"Got mining request for ${requestTransactions.size} new transactions with current blockchain index at ${blockChain.lastBlock.index}")
      val filtered = requestTransactions.filterNot(blockChain.isValidTransaction)

      if ((filtered.toSet -- transactions).nonEmpty) {
        transactions ++= filtered

        network ! BroadcastRequest(AddTransactions(transactions.to[Seq]))

        val miner = createWorker(context)
        context.watch(miner)
        miners += miner

        miner ! MinerActor.MineBlock(blockChain, transactions.to[Seq])
      } else logger.debug("Request contained no new messages, so not doing anything.")
    case BlockChainChanged(newBlockChain) =>
      logger.debug("The blockchain has changed, stopping all miners.")
      miners.foreach(_ ! PoisonPill)

      transactions = transactions.filterNot(newBlockChain.isValidTransaction)

      if (transactions.nonEmpty) {
        self ! MineBlock(newBlockChain, transactions.to[Seq])
      }
    case MineResult(block) =>
      logger.debug(s"Received a valid block from the miner for index ${block.index}, adding it to the chain.")
      network ! BlockChainActor.NewBlock(block)
    case Terminated(deadMiner) =>
      miners -= deadMiner
      logger.debug(s"${transactions.size} transactions being processed by ${miners.size} miners")

      if (miners.isEmpty && transactions.nonEmpty) {
        val savedMessages = transactions.to[Seq]
        transactions = Set.empty
        (network ? GetBlockChain).mapTo[CurrentBlockChain].map(r => MineBlock(r.blockChain, savedMessages)).pipeTo(self)
      }
  }
}
