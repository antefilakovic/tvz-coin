package dev.afilakovic.crypto

import java.math.BigInteger
import java.security.MessageDigest

import dev.afilakovic.blockchain.{Block, Transaction, TransactionInput, TransactionOutput}

//todo: use config loader for default algorithm
object Hashing{
  val DEFAULT_ALGORITHM = "SHA-256"
  val DEFAULT_CHARSET = "UTF-8"

  def apply(algorithm: String = DEFAULT_ALGORITHM, charset: String = DEFAULT_CHARSET) = new Hashing(algorithm, charset)
}

class Hashing private(algorithm: String, charset: String){
  private val digest = MessageDigest.getInstance(algorithm)

  implicit def byteArrayToBigInt(byteArray: Array[Byte]): BigInt = new BigInt(new BigInteger(byteArray))

  private def hash[A](input: A) = digest.digest(input.toString.getBytes(charset))

  def hashTransactionInput(input: TransactionInput): BigInt = hash(input)

  def hashTransactionOutput(transactionOutput: TransactionOutput): BigInt = hash(transactionOutput)

  def hashTransaction(transaction: Transaction): BigInt = hash(transaction)

  def hashBlock(block: Block): BigInt = hash(block)
}

