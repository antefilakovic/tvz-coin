package dev.afilakovic.blockchain

import java.time.LocalDateTime

import dev.afilakovic.AppConfig
import dev.afilakovic.crypto.Hashing

case class Block(index: Long,
                 previousHash: BigInt,
                 transactions: Seq[Transaction],
                 nonce: Long) {
  val hash = Hashing().hashBlock(this)
  val timestamp: LocalDateTime = LocalDateTime.now

  def transactionInputsByUser(user: String) = transactions.flatMap(_.input).filter(_.payer == user)

  def transactionOutputsByUser(user: String) = transactions.flatMap(_.output).filter(_.payee == user)

  def unspentTransactionOutputsByUser(user: String, spentTransactions: Seq[TransactionInput]) =
    transactionOutputsByUser(user).filter(output => spentTransactions.map(_.outputHash).exists(_.equals(output.hash)))

  override def toString: String = List(index, previousHash, transactions.map(_.hash).mkString(AppConfig.DEFAULT_STRING_DELIMITER), nonce).mkString(AppConfig.DEFAULT_STRING_DELIMITER)
}

object GenesisBlock extends Block(0, BigInt(0), Seq(new BlockReward("")), 0)

