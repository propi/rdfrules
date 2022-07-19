package com.github.propi.rdfrules.gui

sealed trait OperationStructure

object OperationStructure {

  object Empty extends OperationStructure {
    override def toString: String = "Empty"
  }

  object Dataset extends OperationStructure {
    override def toString: String = "Dataset"
  }

  object Index extends OperationStructure {
    override def toString: String = "Index"
  }

  object Ruleset extends OperationStructure {
    override def toString: String = "Ruleset"
  }

  object Prediction extends OperationStructure {
    override def toString: String = "Prediction"
  }

}