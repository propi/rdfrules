package com.github.propi.rdfrules.rule

trait PatternMatcher[T, P] {
  def matchPattern(x: T, pattern: P): Boolean
}