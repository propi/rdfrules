package com.github.propi.rdfrules.http.task

sealed trait ShrinkSetup

object ShrinkSetup {
  case class Drop(n: Int) extends ShrinkSetup

  case class Take(n: Int) extends ShrinkSetup

  case class Slice(from: Int, until: Int) extends ShrinkSetup
}