package dev.afilakovic.blockchain

import java.math.BigInteger

import dev.afilakovic.AppConfig
import dev.afilakovic.crypto.Hashing
import io.circe.Encoder

case class Block(index: Long,
                 previousHash: BigInt,
                 signedTransaction: Set[SignedTransaction],
                 nonce: Long,
                 timestamp: Long = System.currentTimeMillis()) {
  val hash = Hashing().hashBlock(this)

  def transactionInputsByUser(user: String) = signedTransaction.map(_.transaction).flatMap(_.input).filter(_.payer == user)

  def transactionOutputsByUser(user: String) = signedTransaction.map(_.transaction).flatMap(_.output).filter(_.payee == user)

  override def toString: String =
    List(index, previousHash, signedTransaction.map(_.toString).mkString(AppConfig.DEFAULT_STRING_DELIMITER), nonce).mkString(AppConfig.DEFAULT_STRING_DELIMITER)
}

object BlockEncode {
  import TransactionEncode._

  implicit val encodeBlock: Encoder[Block] =
    Encoder.forProduct5("index", "previousHash", "signedTransaction", "nonce", "hash")(block =>
      (block.index, String.format("%032x", new BigInteger(1, block.previousHash.toByteArray)), block.signedTransaction, block.nonce, String.format("%032x", new BigInteger(1, block.hash.toByteArray)))
    )
}

object GenesisBlock extends Block(0, BigInt(0), Set(BlockReward.first), 0, 1535932800000L)

