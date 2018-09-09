package dev.afilakovic

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import dev.afilakovic.blockchain._
import dev.afilakovic.crypto.DigitalSignature
import dev.afilakovic.p2p.BlockChainActor._
import dev.afilakovic.p2p.NetworkActor
import dev.afilakovic.p2p.NetworkActor.{AddPeer, AddTransactions}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object AppConfig {
  val DEFAULT_STRING_DELIMITER = "$%#"

  val config = ConfigFactory.load()
  val SEED_HOST = config.getString("network.seed")

  val privateKeyLocation = config.getString("crypto.keys.path.key")
  val publicKeyLocation = config.getString("crypto.keys.path.pub")

  val SIGNATURE: DigitalSignature = if (privateKeyLocation.isEmpty || publicKeyLocation.isEmpty) DigitalSignature.apply else DigitalSignature(privateKeyLocation, publicKeyLocation)
}

trait RestInterface extends FailFastCirceSupport {

  import BlockEncode._
  import TransactionEncode._
  import akka.http.scaladsl.server.Directives._
  import akka.pattern.ask
  import io.circe.syntax._

  val networkActor: ActorRef
  val logger: Logger

  implicit val executionContext: ExecutionContext
  implicit val timeout = Timeout(30, TimeUnit.SECONDS)

  def routes = get {
    respondWithHeaders(RawHeader("Access-Control-Allow-Origin", "*")) {
      pathPrefix("v1") {
        pathPrefix("blocks") {
          pathEndOrSingleSlash {
            get {
              complete((networkActor ? GetBlockChain).mapTo[CurrentBlockChain].map(_.blockChain.list.asJson))
            }
          } ~ pathPrefix("last") {
            get {
              complete((networkActor ? GetLast).mapTo[NewBlock].map(_.block.asJson))
            }
          }
        } ~ pathPrefix("balance") {
          get {
            complete((networkActor ? GetBalance).mapTo[Balance].map(bal => bal.balance.asJson))
          }
        } ~ pathPrefix("transaction") {
          pathPrefix(Segment / Segment) { (address, amount) =>
            get {
              val result = (networkActor ? CreateTransaction(BigDecimal(amount), address)).mapTo[TransactionCreated]
                .map(_.signedTx)
                .map {
                  case Success(tx) => networkActor ! AddTransactions(Seq(tx))
                    (1, tx.asJson).asJson
                  case Failure(e) => logger.error("Error occured while trying to create new transaction", e)
                    (0, e.getMessage).asJson
                }
              complete(result)
            }
          }
        } ~ pathPrefix("address") {
          pathEndOrSingleSlash {
            get {
              complete(AppConfig.SIGNATURE.address)
            }
          }
        }
      } ~
        pathPrefix("ping") {
          get {
            complete("OK")
          }
        }
    }
  }

}

object TvzCoinApp extends App with RestInterface {

  import dev.afilakovic.AppConfig.SEED_HOST

  val logger = Logger("TvzCoin")

  implicit val system = ActorSystem("TvzCoin")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val networkActor = system.actorOf(NetworkActor.props, "networkActor")

  logger.info(s"User <${AppConfig.SIGNATURE.address}> startup.")

  if (SEED_HOST.nonEmpty) {
    logger.info(s"Attempting to connect to seed-host <$SEED_HOST>")
    networkActor ! AddPeer(SEED_HOST)
  } else {
    logger.info("No seed host configured.")
  }

  Http().bindAndHandle(routes, "0.0.0.0", 8080)
}
