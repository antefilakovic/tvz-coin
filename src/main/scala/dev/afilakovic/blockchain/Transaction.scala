package dev.afilakovic.blockchain

import java.nio.file.{Files, Paths}
import java.security.{InvalidParameterException, PublicKey}
import java.util.UUID

import dev.afilakovic.AppConfig
import dev.afilakovic.crypto.{DigitalSignature, Hashing}
import io.circe.generic.JsonCodec

import scala.util.{Failure, Success, Try}

object TransactionConstants {
  val HASHING = Hashing()
  val BLOCK_REWARD_AMOUNT = 25
  val GENESIS_BLOCK_PUB_KEY = "src/main/resources/genesis-block.pub"
  val GENESIS_BLOCK_SIGNATURE = "src/main/resources/genesis-block.sig"
  val GENESIS_BLOCK_ADDRESS = "d870c2cd783fd5ad983c9c1d67cb613a79f9effe84d364323eea74e92d57c10f"
}

object TransactionInput {
  def apply(output: TransactionOutput) = new TransactionInput(output.hash, output.amount, output.payee)
}

@JsonCodec
case class TransactionInput private(outputHash: BigInt,
                                    amount: BigDecimal,
                                    payer: String) {
  val id = UUID.randomUUID().toString
  val hash = TransactionConstants.HASHING.hashTransactionInput(this)

  override def toString: String = List(id, outputHash, amount, payer).mkString(AppConfig.DEFAULT_STRING_DELIMITER)
}

@JsonCodec
case class TransactionOutput(amount: BigDecimal,
                             payee: String) {
  val id = UUID.randomUUID().toString
  val hash = TransactionConstants.HASHING.hashTransactionOutput(this)

  override def toString: String = List(id, amount, payee).mkString(AppConfig.DEFAULT_STRING_DELIMITER)
}

@JsonCodec
case class Transaction(input: Seq[TransactionInput],
                       output: Seq[TransactionOutput],
                       timestamp: Long = System.currentTimeMillis()) {
  val hash = TransactionConstants.HASHING.hashTransaction(this)

  override def toString: String = List(output.map(_.hash).mkString(AppConfig.DEFAULT_STRING_DELIMITER), input.map(_.hash).mkString(AppConfig.DEFAULT_STRING_DELIMITER), timestamp)
    .mkString(AppConfig.DEFAULT_STRING_DELIMITER)
}

object TransactionCreator {
  def apply(input: Seq[TransactionOutput], amount: BigDecimal, payee: String): Try[SignedTransaction] = {
    val address = AppConfig.SIGNATURE.address
    if (!input.map(_.payee).forall(address.equals)) {
      return Failure(new InvalidParameterException("Invalid input transactions chosen, you are not owner of all of them."))
    }

    val inputAmount = input.map(_.amount).sum

    if (inputAmount < amount) {
      Failure(new InvalidParameterException("Insufficient funds."))
    } else {
      val inputs = input.map(TransactionInput(_))
      val outputs = if (inputAmount == amount) Seq(TransactionOutput(amount, payee)) else Seq(TransactionOutput(amount, payee), TransactionOutput(inputAmount - amount, address))
      val transaction = Transaction(inputs, outputs)
      val signature = AppConfig.SIGNATURE.sign(transaction)
      Success(SignedTransaction(AppConfig.SIGNATURE.publicKey.getEncoded, signature, transaction))
    }
  }
}

@JsonCodec
case class SignedTransaction(publicKey: Array[Byte], signature: Array[Byte], transaction: Transaction) {
  override def toString: String = List(publicKey, signature, transaction.hash).mkString(AppConfig.DEFAULT_STRING_DELIMITER)
}

object BlockReward {
  def create = {
    val transaction = Transaction(Seq.empty[TransactionInput], Seq(TransactionOutput(TransactionConstants.BLOCK_REWARD_AMOUNT, AppConfig.SIGNATURE.address)))
    val signature = AppConfig.SIGNATURE.sign(transaction)
    new BlockReward(AppConfig.SIGNATURE.publicKey, signature, transaction)
  }

  def first = {
    import dev.afilakovic.blockchain.TransactionConstants._

    val transaction = Transaction(Seq.empty[TransactionInput], Seq(TransactionOutput(BLOCK_REWARD_AMOUNT, GENESIS_BLOCK_ADDRESS)), 1535932800000L)
    val pubKey = DigitalSignature.loadPublicKey(GENESIS_BLOCK_PUB_KEY)
    val signature = Files.readAllBytes(Paths.get(GENESIS_BLOCK_SIGNATURE))
    new BlockReward(pubKey, signature, transaction)
  }
}

class BlockReward private(publicKey: PublicKey, signature: Array[Byte], transaction: Transaction)
  extends SignedTransaction(publicKey.getEncoded, signature, transaction)
