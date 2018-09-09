package dev.afilakovic.blockchain

import dev.afilakovic.AppConfig
import dev.afilakovic.crypto.{DigitalSignature, Hashing}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object BlockChain {
  val HASHING = Hashing()
  val DIFFICULTY = BigInt(10).pow(72).toDouble

  def proofOfWork(block: Block) = block.hash.abs.toDouble < BlockChain.DIFFICULTY

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

  def unspentTransactionsForUser(user: String): List[TransactionOutput] = {
    val transactions = list.reverse.flatMap(_.signedTransaction).map(_.transaction)
    val address = AppConfig.SIGNATURE.address
    val (inputs, outputs) = (
      transactions.flatMap(_.input).filter(_.payer == address), transactions.flatMap(_.output).filter(_.payee == address)
    )
    println(s"Found <${inputs.size} inputs with hashes <${inputs.map(_.outputHash).mkString(", ")}>")
    println(s"Found <${outputs.size} outputs with hashes <${outputs.map(_.hash).mkString(", ")}>")
    val found = outputs.filterNot(o => inputs.map(_.outputHash).contains(o.hash)).toSet
    println(s"Found ${found.size} valid outputs with amounts : <${found.toList.map(_.amount).mkString(", ")}>")
    found.toList
  }

  def appendBlocks(newBlocks: Seq[Block]): Try[BlockChain] = newBlocks.foldLeft(Try(this)) {
    (thisChain, block) => thisChain.flatMap(chain => chain.addBlock(block))
  }

  def addBlock(block: Block): Try[BlockChain] =
    if (validBlock(block)) Success(new BlockChain(block, Some(this))) else Failure(new IllegalArgumentException("Invalid block added"))

  def validBlock(newBlock: Block): Boolean = validBlock(newBlock, head)

  private def validBlock(block: Block, previousBlock: Block) =
    previousBlock.index + 1 == block.index &&
      previousBlock.hash == block.previousHash &&
      previousBlock.timestamp < block.timestamp &&
      BlockChain.HASHING.hashBlock(block) == block.hash &&
      areAllTransactionsValid(block) &&
      BlockChain.proofOfWork(block)

  private def areAllTransactionsValid(block: Block): Boolean = {
    val blockRewards = block.signedTransaction.filter(_.isInstanceOf[BlockReward])
    blockRewards.size == 1 && (block.signedTransaction -- blockRewards).forall(isTransactionValid)
  }

  def isTransactionValid(signedTransaction: SignedTransaction): Boolean = {
    val transaction = signedTransaction.transaction
    areAllInputsUnspent(transaction) &&
      transaction.input.forall(isThereAnOutputForInput) &&
      DigitalSignature.verify(signedTransaction)
  }

  def isThereAnOutputForInput(input: TransactionInput): Boolean = foldLeft[Boolean](false) {
    (stmt, block) =>
      stmt ||
        block.signedTransaction.map(_.transaction).flatMap(_.output).map(_.hash).contains(input.outputHash)
  }

  private def areAllInputsUnspent(transaction: Transaction) = foldLeft[Boolean](true) {
    (stmt, block) =>
      stmt &&
        !block.signedTransaction.map(_.transaction).flatMap(_.input).map(_.outputHash).exists(usedOutput => transaction.input.map(_.outputHash).contains(usedOutput))
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
