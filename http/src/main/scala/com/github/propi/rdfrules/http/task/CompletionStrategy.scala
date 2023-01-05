package com.github.propi.rdfrules.http.task

sealed trait CompletionStrategy extends Product

object CompletionStrategy {
  case object DistinctPredictions extends CompletionStrategy

  case object FunctionalPredictions extends CompletionStrategy

  case object PcaPredictions extends CompletionStrategy

  case object QpcaPredictions extends CompletionStrategy
}