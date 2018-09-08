package dev.afilakovic.crypto

import java.nio.file.{Files, Paths}
import java.security._
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.util.Base64

import dev.afilakovic.blockchain.{SignedTransaction, Transaction}
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECParameterSpec

object DigitalSignature {
  val bcProvider = new BouncyCastleProvider
  val ecdsaSign = Signature.getInstance("SHA256withECDSA", bcProvider)

  private def generateKeyPair = {
    val ecP = CustomNamedCurves.getByName("curve25519")
    val ecSpec = new ECParameterSpec(ecP.getCurve, ecP.getG, ecP.getN, ecP.getH, ecP.getSeed)
    val generator: KeyPairGenerator = KeyPairGenerator.getInstance("ECDSA", bcProvider)
    generator.initialize(ecSpec)
    generator.generateKeyPair
  }

  private def sign(transaction: Transaction, privateKey: PrivateKey) = {
    ecdsaSign.initSign(privateKey)
    ecdsaSign.update(transaction.hash.toString.getBytes("UTF-8"))
    ecdsaSign.sign
  }

  def verify(signedTx: SignedTransaction) = {
    ecdsaSign.initVerify(loadPublicKeyFromBytes(signedTx.publicKey))
    ecdsaSign.update(signedTx.transaction.hash.toString.getBytes("UTF-8"))
    ecdsaSign.verify(signedTx.signature)
  }

  def apply = {
    val keyPair: KeyPair = generateKeyPair
    new DigitalSignature(keyPair.getPrivate, keyPair.getPublic)
  }

  def apply(privKeyLocation: String, pubKeyLocation: String) = new DigitalSignature(loadPrivateKey(privKeyLocation), loadPublicKey(pubKeyLocation))

  private def loadPrivateKey(privKeyLocation: String) = {
    val privateKeyBytes = Files.readAllBytes(Paths.get(privKeyLocation))
    val keySpec = new PKCS8EncodedKeySpec(privateKeyBytes)
    val kf = KeyFactory.getInstance("ECDSA", bcProvider)
    kf.generatePrivate(keySpec)
  }

  def loadPublicKey(pubKeyLocation: String) = {
    val publicKeyBytes = Files.readAllBytes(Paths.get(pubKeyLocation))
    loadPublicKeyFromBytes(publicKeyBytes)
  }

  private def loadPublicKeyFromBytes(byteArray: Array[Byte]) = {
    val keySpec = new X509EncodedKeySpec(byteArray)
    val kf = KeyFactory.getInstance("ECDSA", bcProvider)
    kf.generatePublic(keySpec)
  }
}

case class DigitalSignature private(privKey: PrivateKey, pubKey: PublicKey) {
  val address: String = Hashing().hashAddress(Base64.getEncoder.encodeToString(pubKey.getEncoded))

  def publicKey = pubKey

  def sign(transaction: Transaction) = DigitalSignature.sign(transaction, privKey)
}
