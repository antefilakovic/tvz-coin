package dev.afilakovic.blockchain

import java.math.BigInteger
import java.nio.file.{Files, Paths}
import java.security.{InvalidParameterException, PublicKey}

import dev.afilakovic.AppConfig
import dev.afilakovic.crypto.{DigitalSignature, Hashing}
import io.circe.Encoder

import scala.util.{Failure, Success, Try}

object TransactionConstants {
  val HASHING = Hashing()
  val BLOCK_REWARD_AMOUNT = 25
  val GENESIS_BLOCK_PUB_KEY = "src/main/resources/genesis-block.pub"
  val GENESIS_BLOCK_SIGNATURE = "src/main/resources/genesis-block.sig"
  val GENESIS_BLOCK_ADDRESS = "9207b3afe1e404b5df104149e5278515cb19f5506d50dfea15638f7b8a0cbe43"
}

object TransactionInput {
  def apply(output: TransactionOutput) = new TransactionInput(output.hash, output.amount, output.payee)
}

case class TransactionInput private(outputHash: BigInt,
                                    amount: BigDecimal,
                                    payer: String) {
  val hash = TransactionConstants.HASHING.hashTransactionInput(this)

  override def toString: String = List(outputHash, amount, payer).mkString(AppConfig.DEFAULT_STRING_DELIMITER)
}

case class TransactionOutput(amount: BigDecimal,
                             payee: String,
                             timestamp: Long = System.currentTimeMillis()) {
  val hash = TransactionConstants.HASHING.hashTransactionOutput(this)

  override def toString: String = List(amount, payee, timestamp).mkString(AppConfig.DEFAULT_STRING_DELIMITER)
}

case class Transaction(input: Seq[TransactionInput],
                       output: Seq[TransactionOutput],
                       timestamp: Long = System.currentTimeMillis()) {
  val hash = TransactionConstants.HASHING.hashTransaction(this)

  override def toString: String = List(output.map(_.hash).mkString(AppConfig.DEFAULT_STRING_DELIMITER), input.map(_.hash).mkString(AppConfig.DEFAULT_STRING_DELIMITER), timestamp)
    .mkString(AppConfig.DEFAULT_STRING_DELIMITER)
}

object TransactionEncode{
  implicit val encodeTx: Encoder[Transaction] =
    Encoder.forProduct1("hash")(tx => String.format("%032x", new BigInteger(1, tx.hash.toByteArray)))

  implicit val encodeSignedTx: Encoder[SignedTransaction] =
    Encoder.forProduct1("transaction")(tx => tx.transaction)
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

case class SignedTransaction(publicKey: Array[Byte], signature: Array[Byte], transaction: Transaction) {
  override def toString: String = List(String.format("%032x", new BigInteger(1, publicKey)), String.format("%032x", new BigInteger(1, signature)), transaction.hash).mkString(AppConfig.DEFAULT_STRING_DELIMITER)

  override def equals(obj: scala.Any): Boolean = obj.isInstanceOf[SignedTransaction] && transaction.hash == obj.asInstanceOf[SignedTransaction].transaction.hash

  override def hashCode(): Int = transaction.hash.toInt
}

object BlockReward {
  def create = {
    val transaction = Transaction(Seq.empty[TransactionInput], Seq(TransactionOutput(TransactionConstants.BLOCK_REWARD_AMOUNT, AppConfig.SIGNATURE.address)))
    val signature = AppConfig.SIGNATURE.sign(transaction)
    new BlockReward(AppConfig.SIGNATURE.publicKey, signature, transaction)
  }

  def first = {
    import dev.afilakovic.blockchain.TransactionConstants._

    val transaction = Transaction(Seq.empty[TransactionInput], Seq(TransactionOutput(BLOCK_REWARD_AMOUNT, GENESIS_BLOCK_ADDRESS, 1535932800000L)), 1535932800000L)
    val pubKey = DigitalSignature.loadPublicKey(GENESIS_BLOCK_PUB_KEY)
    val signature = Files.readAllBytes(Paths.get(GENESIS_BLOCK_SIGNATURE))
    new BlockReward(pubKey, signature, transaction)
  }
}

class BlockReward private(publicKey: PublicKey, signature: Array[Byte], transaction: Transaction)
  extends SignedTransaction(publicKey.getEncoded, signature, transaction)
