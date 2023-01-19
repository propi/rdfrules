package com.github.propi.rdfrules.utils

trait Functor[+T] {
  self =>

  def apply(): T

  def map[A](f: T => A): Functor[A] = () => f(self())

  def mapIf[A >: T](f: T => Boolean, g: T => A): Functor[A] = if (f(self())) map(g) else this

  def orElse[A >: T](f: T => Boolean, alt: => A): Functor[A] = map(x => if (f(x)) x else alt)

  def collect(f: PartialFunction[T, _]): Unit = if (f.isDefinedAt(apply())) {
    f.apply(apply())
  }

  def foreach(f: T => Unit): Unit = f(self())
}

object Functor {
  def apply[T](x: T): Functor[T] = () => x
}