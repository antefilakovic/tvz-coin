package dev.afilakovic.blockchain

import dev.afilakovic.AppConfig
import dev.afilakovic.crypto.Hashing
import io.circe.generic.JsonCodec

@JsonCodec
case class Block(index: Long,
                 previousHash: BigInt,
                 signedTransaction: Set[SignedTransaction],
                 nonce: Long,
                 timestamp: Long = System.currentTimeMillis()) {
  val hash = Hashing().hashBlock(this)

  def transactionInputsByUser(user: String) = signedTransaction.map(_.transaction).flatMap(_.input).filter(_.payer == user)

  def transactionOutputsByUser(user: String) = signedTransaction.map(_.transaction).flatMap(_.output).filter(_.payee == user)

  def unspentTransactionOutputsByUser(user: String, spentTransactions: Set[TransactionInput]) =
    transactionOutputsByUser(user).filter(output => spentTransactions.map(_.outputHash).exists(_.equals(output.hash)))

  override def toString: String =
    List(index, previousHash, signedTransaction.map(_.toString).mkString(AppConfig.DEFAULT_STRING_DELIMITER), nonce).mkString(AppConfig.DEFAULT_STRING_DELIMITER)
}

object GenesisBlock extends Block(0, BigInt(0), Set(BlockReward.first), 0, 1535932800000L)

