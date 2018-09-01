package dev.afilakovic.blockchain

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object BlockChain {
  def apply() = new BlockChain(GenesisBlock, None)
}

class BlockChain private(head: Block, tail: Option[BlockChain]) {
  def list: List[Block] = foldLeft[List[Block]](Nil) {
    (list, block) => block :: list
  }.reverse

  def unspentTransactionsForUser(user: String): Seq[TransactionOutput] =
    foldLeft[(Seq[TransactionInput], Seq[TransactionOutput])]((Seq.empty[TransactionInput], Seq.empty[TransactionOutput])) {
      (tuple, block) => (block.transactionInputsByUser(user) ++ tuple._1, block.unspentTransactionOutputsByUser(user, tuple._1) ++ tuple._2)
    }._2

  def createWith(blocks: Seq[Block]): Try[BlockChain] = blocks match {
    case GenesisBlock :: otherBlocks => BlockChain().appendBlocks(otherBlocks)
    case _ => Failure(new IllegalArgumentException("Chain must begin with the GenesisBlock"))
  }

  def appendBlocks(newBlocks: Seq[Block]): Try[BlockChain] = newBlocks.foldLeft(Try(this)) {
    (thisChain, block) => thisChain.flatMap(chain => chain.addBlock(block))
  }

  def addBlock(block: Block): Try[BlockChain] =
    if (validBlock(block)) Success(new BlockChain(block, Some(this)))
    else Failure(new IllegalArgumentException("Invalid block added"))

  def validBlock(newBlock: Block): Boolean = validBlock(newBlock, head)

  private def validBlock(newBlock: Block, previousBlock: Block) =
    previousBlock.index + 1 == newBlock.index &&
      previousBlock.hash == newBlock.previousHash //TODO: finish this function

  @tailrec
  final def valid: Boolean = tail match {
    case None => head == GenesisBlock
    case Some(chain) => validBlock(head, chain.head) && chain.valid
  }

  @tailrec
  final def foldLeft[A](startingValue: A)(operation: (A, Block) => A): A = {
    val result = operation(startingValue, head)
    tail match {
      case None => result
      case Some(blockChain) => blockChain.foldLeft(result)(operation)
    }
  }
}
