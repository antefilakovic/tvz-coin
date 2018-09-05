package dev.afilakovic.blockchain

import java.time.LocalDateTime
import java.util.UUID

import dev.afilakovic.AppConfig
import dev.afilakovic.crypto.Hashing

object TransactionConstants {
  val BLOCK_REWARD_AMOUNT = 25
  val HASHING = Hashing()
}

case class TransactionInput(outputHash: BigInt,
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

//todo: add digital signatures maybe
case class Transaction(input: Seq[TransactionInput],
                       output: Seq[TransactionOutput]) {
  val timestamp: LocalDateTime = LocalDateTime.now
  val hash = TransactionConstants.HASHING.hashTransaction(this)

  override def toString: String = List(output.map(_.hash).mkString(AppConfig.DEFAULT_STRING_DELIMITER), input.map(_.hash).mkString(AppConfig.DEFAULT_STRING_DELIMITER), timestamp)
    .mkString(AppConfig.DEFAULT_STRING_DELIMITER)
}

class BlockReward(blockCreator: String) extends Transaction(Seq.empty[TransactionInput], Seq(TransactionOutput(TransactionConstants.BLOCK_REWARD_AMOUNT, blockCreator)))
