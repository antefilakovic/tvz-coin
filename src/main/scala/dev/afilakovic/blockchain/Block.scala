package dev.afilakovic.blockchain

import dev.afilakovic.crypto.Hashing

case class Block(index: Long,
                 previousHash: BigInt,
                 transactions: Seq[Transaction],
                 nonce: Long) {
  val hash = Hashing().hashBlock(this)//TODO

  def transactionInputsByUser(user: String) = transactions.flatMap(_.input).filter(_.payer == user)

  def transactionOutputsByUser(user: String) = transactions.flatMap(_.output).filter(_.payee == user)

  def unspentTransactionOutputsByUser(user: String, spentTransactions: Seq[TransactionInput]) =
    transactionOutputsByUser(user).filter(output => spentTransactions.map(_.outputHash).exists(_.equals(output.hash)))

  override def toString: String = index + "$%#" + previousHash + "$%#" + transactions.map(_.hash).mkString("$%#") + "$%#" + nonce
}

object GenesisBlock extends Block(0, BigInt(0), Seq(new BlockReward("")), 0)

