package dev.afilakovic.blockchain

import java.time.LocalDateTime
import java.util.UUID

object TransactionConstants{
  val BLOCK_REWARD_AMOUNT = 25
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
  val hash = BigInt(1) //TODO
  val signature = BigInt(1)//TODO
}

case class Transaction private(input: Seq[TransactionInput],
                          output: Seq[TransactionOutput]) {
  val hash = BigInt(1) //TODO
}

object BlockReward extends Transaction{
  def apply(blockCreator: String) = Transaction(Seq.empty[TransactionInput], Seq(TransactionOutput(TransactionConstants.BLOCK_REWARD_AMOUNT, blockCreator)))
}
