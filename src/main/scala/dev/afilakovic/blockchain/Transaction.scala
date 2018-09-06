package dev.afilakovic.blockchain

import java.security.{InvalidParameterException, PublicKey}
import java.time.LocalDateTime
import java.util.UUID

import dev.afilakovic.AppConfig
import dev.afilakovic.crypto.Hashing

import scala.util.{Failure, Success, Try}

object TransactionConstants {
  val BLOCK_REWARD_AMOUNT = 25
  val HASHING = Hashing()
}

object TransactionInput {
  def apply(output: TransactionOutput) = new TransactionInput(output.hash, output.amount, output.payee)
}

case class TransactionInput private(outputHash: BigInt,
                                    amount: BigDecimal,
                                    payer: String) {
  val id = UUID.randomUUID().toString
  val hash = TransactionConstants.HASHING.hashTransactionInput(this)

  override def toString: String = List(id, outputHash, amount, payer).mkString(AppConfig.DEFAULT_STRING_DELIMITER)
}

case class TransactionOutput(amount: BigDecimal,
                             payee: String) {
  val id = UUID.randomUUID().toString
  val hash = TransactionConstants.HASHING.hashTransactionOutput(this)

  override def toString: String = List(id, amount, payee).mkString(AppConfig.DEFAULT_STRING_DELIMITER)
}

case class Transaction(input: Seq[TransactionInput],
                       output: Seq[TransactionOutput],
                       timestamp: LocalDateTime = LocalDateTime.now) {
  val hash = TransactionConstants.HASHING.hashTransaction(this)

  override def toString: String = List(output.map(_.hash).mkString(AppConfig.DEFAULT_STRING_DELIMITER), input.map(_.hash).mkString(AppConfig.DEFAULT_STRING_DELIMITER), timestamp)
    .mkString(AppConfig.DEFAULT_STRING_DELIMITER)
}

object Transaction {
  def apply(input: Seq[TransactionOutput], amount: BigDecimal, payee: String): Try[SignedTransaction] = {
    val payers = input.map(_.payee).distinct
    if (payers.size != 1) {
      return Failure(new InvalidParameterException("Invalid input transactions chosen, only one user can pay."))
    }

    val payer = payers.head
    val inputAmount = input.map(_.amount).sum

    if (inputAmount < amount) {
      Failure(new InvalidParameterException("Insufficient funds."))
    } else if (inputAmount == amount) {
      val inputs = input.map(TransactionInput(_))
      val outputs = Seq(TransactionOutput(amount, payee))
      val transaction = new Transaction(inputs, outputs)
      val signature = AppConfig.SIGNATURE.sign(transaction)
      Success(SignedTransaction(AppConfig.SIGNATURE.publicKey, signature, transaction))
    } else {
      val inputs = input.map(TransactionInput(_))
      val outputs = Seq(TransactionOutput(amount, payee), TransactionOutput(inputAmount - amount, payer))
      val transaction = new Transaction(inputs, outputs)
      val signature = AppConfig.SIGNATURE.sign(transaction)
      Success(SignedTransaction(AppConfig.SIGNATURE.publicKey, signature, transaction))
    }
  }
}

case class SignedTransaction(publicKey: PublicKey, signature: Array[Byte], transaction: Transaction) {
  override def toString: String = List(publicKey, signature, transaction.hash).mkString(AppConfig.DEFAULT_STRING_DELIMITER)
}

object BlockReward {
  def create = {
    val transaction = Transaction(Seq.empty[TransactionInput], Seq(TransactionOutput(TransactionConstants.BLOCK_REWARD_AMOUNT, AppConfig.SIGNATURE.address)))
    val signature = AppConfig.SIGNATURE.sign(transaction)
    new BlockReward(AppConfig.SIGNATURE.publicKey, signature, transaction)
  }

  def first = {
    val transaction = Transaction(Seq.empty[TransactionInput], Seq(TransactionOutput(TransactionConstants.BLOCK_REWARD_AMOUNT, "user1")))
    new BlockReward(null, null, transaction)
  }
}

class BlockReward private(publicKey: PublicKey, signature: Array[Byte], transaction: Transaction)
  extends SignedTransaction(publicKey, signature, transaction)
