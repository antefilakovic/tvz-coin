package dev.afilakovic

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import dev.afilakovic.crypto.DigitalSignature
import dev.afilakovic.p2p.NetworkActor
import dev.afilakovic.p2p.NetworkActor.AddPeer

object AppConfig {
  val DEFAULT_STRING_DELIMITER = "$%#"

  val config = ConfigFactory.load()
  val SEED_HOST = config.getString("network.seed")
  val DEFAULT_CURVE = config.getString("crypto.default.curve")

  val SIGNATURE = DigitalSignature(DEFAULT_CURVE)
}

object TvzCoinApp extends App {
  import dev.afilakovic.AppConfig.SEED_HOST

  val logger = Logger("TvzCoin")

  implicit val system = ActorSystem("TvzCoin")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val networkActor = system.actorOf(NetworkActor.props)

  logger.info(s"User <${AppConfig.SIGNATURE.address}> startup.")

  if (SEED_HOST.nonEmpty) {
    logger.info(s"Attempting to connect to seed-host <$SEED_HOST>")
    networkActor ! AddPeer(SEED_HOST)
  } else {
    logger.info("No seed host configured.")
  }
}
