package com.github.propi.rdfrules.gui

import scala.language.implicitConversions

sealed trait OperationGroup {
  def &+(x: OperationGroup): Set[OperationGroup] = Set(this, x)
}

object OperationGroup {

  sealed trait Structure extends OperationGroup

  object Structure {
    object Dataset extends Structure

    object Index extends Structure

    object Ruleset extends Structure

    object Prediction extends Structure
  }

  object Caching extends OperationGroup

  object Transforming extends OperationGroup

  implicit def operationGroupToGroups(operationGroup: OperationGroup): Set[OperationGroup] = Set(operationGroup)

  implicit class PimpedOperationGroupSet(val groups: Set[OperationGroup]) extends AnyVal {
    def &+(x: OperationGroup): Set[OperationGroup] = groups + x
  }

}
