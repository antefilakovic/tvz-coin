package dev.afilakovic.blockchain

import dev.afilakovic.crypto.Hashing

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object BlockChain {
  val HASHING = Hashing()
  val DIFFICULTY = BigInt(2).pow(250)

  def proofOfWork(block: Block) = block.hash < BlockChain.DIFFICULTY

  def apply() = new BlockChain(GenesisBlock, None)

  def createWith(blocks: Seq[Block]): Try[BlockChain] = blocks match {
    case GenesisBlock :: otherBlocks => BlockChain().appendBlocks(otherBlocks)
    case _ => Failure(new IllegalArgumentException("Chain must begin with the GenesisBlock"))
  }
}

class BlockChain private(head: Block, tail: Option[BlockChain]) {
  def list: List[Block] = foldLeft[List[Block]](Nil) {
    (list, block) => block :: list
  }

  def lastBlock = head

  def unspentTransactionsForUser(user: String): Seq[TransactionOutput] =
    foldLeft[(Seq[TransactionInput], Seq[TransactionOutput])]((Seq.empty[TransactionInput], Seq.empty[TransactionOutput])) {
      (tuple, block) => (block.transactionInputsByUser(user) ++ tuple._1, block.unspentTransactionOutputsByUser(user, tuple._1) ++ tuple._2)
    }._2

  def appendBlocks(newBlocks: Seq[Block]): Try[BlockChain] = newBlocks.foldLeft(Try(this)) {
    (thisChain, block) => thisChain.flatMap(chain => chain.addBlock(block))
  }

  def addBlock(block: Block): Try[BlockChain] =
    if (validBlock(block)) Success(new BlockChain(block, Some(this))) else Failure(new IllegalArgumentException("Invalid block added"))

  def validBlock(newBlock: Block): Boolean = validBlock(newBlock, head)

  private def validBlock(block: Block, previousBlock: Block) =
    previousBlock.index + 1 == block.index &&
      previousBlock.hash == block.previousHash &&
      previousBlock.timestamp.isBefore(block.timestamp) &&
      BlockChain.HASHING.hashBlock(block) == block.hash &&
      block.transactions.forall(isValidTransaction) &&
      BlockChain.proofOfWork(block)

  //todo: add signature check
  def isValidTransaction(transaction: Transaction): Boolean = foldLeft[Boolean](true) {
    (stmt, block) => stmt && !block.transactions.flatMap(_.input).map(_.outputHash).exists(usedOutput => transaction.input.map(_.outputHash).contains(usedOutput))
  }

  @tailrec
  final def valid: Boolean = tail match {
    case None => head == GenesisBlock
    case Some(chain) => validBlock(head, chain.lastBlock) && chain.valid
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
