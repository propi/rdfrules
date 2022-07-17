package com.github.propi.rdfrules.http.task

import com.github.propi.rdfrules.data.ops.Transformable

abstract class CommonShrink[T <: Transformable[_, T]](shrinkSetup: ShrinkSetup) extends Task[T, T] {

  def execute(input: T): T = shrinkSetup match {
    case ShrinkSetup.Drop(n) => input.drop(n)
    case ShrinkSetup.Take(n) => input.take(n)
    case ShrinkSetup.Slice(from, until) => input.slice(from, until)
  }

}