package dev.afilakovic.crypto

import java.math.BigInteger
import java.security.MessageDigest

import dev.afilakovic.blockchain.{Block, Transaction, TransactionInput, TransactionOutput}

object Hashing {
  def apply(algorithm: String = "SHA-256", charset: String = "UTF-8") = new Hashing(algorithm, charset)
}

class Hashing private(algorithm: String, charset: String) {
  private val digest = MessageDigest.getInstance(algorithm)

  implicit def byteArrayToBigInt(byteArray: Array[Byte]): BigInt = new BigInt(new BigInteger(byteArray))

  private def hash[A](input: A) = digest.digest(input.toString.getBytes(charset))

  def hashTransactionInput(input: TransactionInput): BigInt = hash(input)

  def hashTransactionOutput(transactionOutput: TransactionOutput): BigInt = hash(transactionOutput)

  def hashTransaction(transaction: Transaction): BigInt = hash(transaction)

  def hashBlock(block: Block): BigInt = hash(block)

  def hashAddress(pubKeyAsBase64: String): String = String.format("%032x", new BigInteger(1, digest.digest(pubKeyAsBase64.getBytes(charset))))
}

