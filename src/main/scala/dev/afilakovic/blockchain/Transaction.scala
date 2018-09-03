package dev.afilakovic.blockchain

import java.time.LocalDateTime
import java.util.UUID

import dev.afilakovic.crypto.Hashing

object TransactionConstants{
  val BLOCK_REWARD_AMOUNT = 25
  val HASHING = Hashing()
}

case class TransactionInput(outputHash: BigInt,
                            amount: BigDecimal,
                            payer: String){
  val id = UUID.randomUUID().toString
}

case class TransactionOutput(amount: BigDecimal,
                             payee: String){
  val id = UUID.randomUUID().toString
  val timestamp: LocalDateTime = LocalDateTime.now
  val hash = TransactionConstants.HASHING.hashTransactionOutput(this) //TODO
  val signature = BigInt(1)//TODO

  override def toString: String = List(id, amount, payee, timestamp, signature).mkString("$%#")
}

case class Transaction(input: Seq[TransactionInput],
                          output: Seq[TransactionOutput]) {
  val hash =  TransactionConstants.HASHING.hashTransaction(this)//TODO
  override def toString: String = output.map(_.hash).mkString("$%#")
}

class BlockReward(blockCreator: String) extends Transaction(Seq.empty[TransactionInput], Seq(TransactionOutput(TransactionConstants.BLOCK_REWARD_AMOUNT, blockCreator)))
