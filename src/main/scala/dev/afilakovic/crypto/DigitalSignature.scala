package dev.afilakovic.crypto

import java.security._
import java.util.Base64

import dev.afilakovic.blockchain.{SignedTransaction, Transaction}
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECParameterSpec

object DigitalSignature {
  val ecdsaSign = Signature.getInstance("SHA256withECDSA", new BouncyCastleProvider)

  private def generateKeyPair(curve: String) = {
    val ecP = CustomNamedCurves.getByName(curve)
    val ecSpec = new ECParameterSpec(ecP.getCurve, ecP.getG, ecP.getN, ecP.getH, ecP.getSeed)
    val generator: KeyPairGenerator = KeyPairGenerator.getInstance("ECDSA", new BouncyCastleProvider)
    generator.initialize(ecSpec)
    generator.generateKeyPair
  }

  private def sign(transaction: Transaction, privateKey: PrivateKey) = {
    ecdsaSign.initSign(privateKey)
    ecdsaSign.update(transaction.hash.toString.getBytes("UTF-8"))
    ecdsaSign.sign
  }

  def verify(signedTx: SignedTransaction) = {
    ecdsaSign.initVerify(signedTx.publicKey)
    ecdsaSign.update(signedTx.transaction.hash.toString.getBytes("UTF-8"))
    ecdsaSign.verify(signedTx.signature)
  }

  def apply(curve: String) = new DigitalSignature(curve)
}

class DigitalSignature(val curve: String) {

  import dev.afilakovic.crypto.DigitalSignature._

  private val keyPair: KeyPair = generateKeyPair(curve)

  private val privateKey = keyPair.getPrivate
  val publicKey = keyPair.getPublic

  def address: String = Hashing().hashAddress(Base64.getEncoder.encodeToString(publicKey.getEncoded))

  def sign(transaction: Transaction) = DigitalSignature.sign(transaction, privateKey)
}
