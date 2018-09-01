package dev.afilakovic.blockchain

case class Block(index: Long,
                 previousHash: BigInt,
                 transactions: Seq[Transaction],
                 nonce: Long) {
  val hash = BigInt(0) //TODO

  def transactionInputsByUser(user: String) = transactions.flatMap(_.input).filter(_.payer == user)

  def transactionOutputsByUser(user: String) = transactions.flatMap(_.output).filter(_.payee == user)

  def unspentTransactionOutputsByUser(user: String, spentTransactions: Seq[TransactionInput]) =
    transactionOutputsByUser(user).filter(output => spentTransactions.map(_.outputHash).exists(_.equals(output.hash)))
}

//TODO: first block reward and update hash to be valid
object GenesisBlock extends Block(0, BigInt(0), Seq(BlockReward("firstUser")), 0)

