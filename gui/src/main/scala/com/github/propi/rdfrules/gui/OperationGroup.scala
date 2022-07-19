package com.github.propi.rdfrules.gui

import scala.language.implicitConversions

sealed trait OperationGroup {
  def &+(x: OperationGroup): Set[OperationGroup] = Set(this, x)
}

object OperationGroup {

  object Caching extends OperationGroup

  implicit def operationGroupToGroups(operationGroup: OperationGroup): Set[OperationGroup] = Set(operationGroup)

  implicit class PimpedOperationGroupSet(val groups: Set[OperationGroup]) extends AnyVal {
    def &+(x: OperationGroup): Set[OperationGroup] = groups + x
  }

}
