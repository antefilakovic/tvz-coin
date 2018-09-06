package dev.afilakovic.p2p

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.event.LoggingReceive
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import dev.afilakovic.AppConfig
import dev.afilakovic.blockchain.{BlockChain, SignedTransaction}
import dev.afilakovic.p2p.MiningMasterActor.{BlockChainChanged, MineBlock}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

object NetworkActor {

  case class AddPeer(address: String)

  case class ResolvedPeer(actorRef: ActorRef)

  case class HandShake(nodeName: String)

  case object GetPeers

  case class PeersResponse(peers: Seq[String])

  case class BroadcastRequest(request: Any)

  case class BlockChainUpdated(blockChain: BlockChain)

  case class AddTransactions(transactions: Seq[SignedTransaction])

  def props(implicit ec: ExecutionContext) = Props(new NetworkActor)
}

class NetworkActor(implicit ec: ExecutionContext) extends Actor {

  import dev.afilakovic.p2p.BlockChainActor._
  import dev.afilakovic.p2p.NetworkActor._

  val logger: Logger = Logger(classOf[NetworkActor])
  val name = AppConfig.SIGNATURE.address

  implicit val timeout = Timeout(Duration(5, TimeUnit.SECONDS))
  implicit val executionContext = context.system.dispatcher

  val blockChainActor = context.actorOf(BlockChainActor.props(self))
  val miningMaster = context.actorOf(MiningMasterActor.props(self))

  var peers: Set[ActorRef] = Set.empty

  def broadcast(req: Any) = peers.foreach(_ ! req)

  override def receive = LoggingReceive {
    case AddPeer(peerAddress) =>
      logger.debug(s"Request for addition of peer: $peerAddress")
      context.actorSelection(peerAddress).resolveOne().map(ResolvedPeer).pipeTo(self)
    case ResolvedPeer(newPeerRef: ActorRef) =>
      if (!peers.contains(newPeerRef)) {
        context.watch(newPeerRef)

        newPeerRef ! HandShake(name)
        newPeerRef ! GetPeers
        broadcast(AddPeer(newPeerRef.path.toSerializationFormat))
        peers += newPeerRef
        newPeerRef ! BlockChainActor.GetLast

        logger.debug(s"Peers count: ${peers.size}")
      } else logger.debug("Peer already added.")
    case HandShake(fromNode) =>
      logger.debug(s"Handshake from $fromNode at ${sender().path.toStringWithoutAddress}")
      peers += sender()
    case BroadcastRequest(req) => broadcast(req)
    case GetPeers => sender() ! PeersResponse(peers.toSeq.map(_.path.toSerializationFormat))
    case PeersResponse(newPeers) => newPeers.foreach(self ! AddPeer(_))
    case Terminated(actorRef) =>
      logger.debug(s"Peer $actorRef was terminated. Removing it from the list.")
      peers -= actorRef
    case AddTransactions(transactions) =>
      (blockChainActor ? BlockChainActor.GetBlockChain).mapTo[CurrentBlockChain].map(chain => MineBlock(chain.blockChain, transactions.toList)).pipeTo(miningMaster)
    case BlockChainUpdated(blockChain) => miningMaster ! BlockChainChanged(blockChain)
    case req@(BlockChainActor.GetBlockChain | BlockChainActor.GetAllBlocks | BlockChainActor.GetLast | BlockChainActor.NewBlock(_) | BlockChainActor.NewBlockChain(_)) =>
      blockChainActor.forward(req)
  }
}

