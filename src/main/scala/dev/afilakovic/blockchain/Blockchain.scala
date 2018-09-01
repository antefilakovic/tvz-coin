package dev.afilakovic.blockchain

import com.typesafe.scalalogging.Logger

import scala.annotation.tailrec

class Blockchain private(head: Block, tail: Option[Blockchain]) {
  val logger = Logger[Blockchain]

  @tailrec
  final def foldLeft[A](startingValue: A)(operation: (A, Block) => A): A = {
    val result = operation(startingValue, head)
    tail match {
      case None => result
      case Some(blockchain) => blockchain.foldLeft(result)(operation)
    }
  }

  def list: List[Block] = foldLeft[List[Block]](Nil) {
    (list, block) => block :: list
  }.reverse



}
