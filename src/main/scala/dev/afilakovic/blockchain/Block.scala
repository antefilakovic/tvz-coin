package dev.afilakovic.blockchain

case class Block(
                index: Long,
                previousHash: BigInt,
                transactions: Seq[String], //TODO: implement transactions
                nonce: Long,
                hash: BigInt
                )

//TODO: first block reward and update hash to be valid
object GenesisBlock extends Block(0, BigInt(0), Seq.empty[String], 0, BigInt(0))

