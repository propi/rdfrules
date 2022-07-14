package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.rule.Atom

//type VariableMap = Map[Atom.Variable, Atom.Constant]
//TODO check it. These lines are solution for this situation: (?a dbo:officialLanguage ?b) ^ (?a dbo:officialLanguage dbr:English_language) -> (?a dbo:language ?b)
//There can exist two identical mapped atoms - 2x (A dbo:officialLanguage English)
//For this variant it should be banned - so no mapped atom should be returned if the same atom was mapped in the previous rule refinement (it should reduce support and body size)
//We need to check it whether it is a good premision, because some ?a can have more official languages inluding English
//For this variant it should be useful: ( ?c <direction> "east" ) ^ ( ?b <train_id> ?c ) ^ ( ?b <train_id> ?a ) ⇒ ( ?a <direction> "east" )
//ABOVE It is isomorphic group and should be filteren after projections counting
//We partialy solved that by the switch allowDuplicitAtoms. It is true for bodySize counting, but false for projectionBounding and support counzing. Check it whether it is right!
sealed trait VariableMap {
  protected val hmap: Array[Int]

  final protected def addConstant(v: Atom.Variable, c: Atom.Constant): Array[Int] = {
    if (v.index >= hmap.length) {
      val newArray = new Array[Int](v.index + 1)
      Array.copy(hmap, 0, newArray, 0, hmap.length)
      newArray(v.index) = c.value
      newArray
    } else {
      val newArray = hmap.clone()
      newArray(v.index) = c.value
      newArray
    }
  }

  final protected def addConstant(v: Atom.Variable, c: Atom.Constant, v2: Atom.Variable, c2: Atom.Constant): Array[Int] = {
    val maxIndex = math.max(v.index, v2.index)
    if (maxIndex >= hmap.length) {
      val newArray = new Array[Int](maxIndex + 1)
      Array.copy(hmap, 0, newArray, 0, hmap.length)
      newArray(v.index) = c.value
      newArray(v2.index) = c2.value
      newArray
    } else {
      val newArray = hmap.clone()
      newArray(v.index) = c.value
      newArray(v2.index) = c2.value
      newArray
    }
  }

  final def getOrElse[T >: Atom.Constant](key: Atom.Variable, default: => T): T = {
    if (contains(key)) {
      Atom.Constant(hmap(key.index))
    } else {
      default
    }
  }

  final def contains(key: Atom.Variable): Boolean = key.index < hmap.length && hmap(key.index) != 0

  final def containsConstant(x: Int): Boolean = hmap.contains(x)

  /**
    * Specify item from variableMap.
    * If the item is not included in variableMap it returns original item otherwise returns constant
    *
    * @param item item to be specified
    * @return specified item or original item if it is not included in the variable map
    */
  final def specifyItem(item: Atom.Item): Atom.Item = item match {
    case x: Atom.Variable => this.getOrElse(x, x)
    case x => x
  }

  final def specifyAtom(atom: Atom): Atom = Atom(specifyItem(atom.subject), atom.predicate, specifyItem(atom.`object`))

  final def apply(key: Atom.Variable): Atom.Constant = Atom.Constant(hmap(key.index))

  final def get(key: Atom.Variable): Option[Atom.Constant] = if (contains(key)) Some(apply(key)) else None

  final def isEmpty: Boolean = hmap.isEmpty

  def containsAtom(atom: Atom): Boolean

  def +(s: (Atom.Variable, Atom.Constant), p: Int, o: (Atom.Variable, Atom.Constant)): VariableMap

  def +(s: Atom.Constant, p: Int, o: (Atom.Variable, Atom.Constant)): VariableMap

  def +(s: (Atom.Variable, Atom.Constant), p: Int, o: Atom.Constant): VariableMap

  def +(s: Atom.Constant, p: Int, o: Atom.Constant): VariableMap

  def injectiveMapping: Boolean
}

object VariableMap {

  private class InjectiveMapping(protected val hmap: Array[Int], atoms: Set[Atom]) extends VariableMap {
    def containsAtom(atom: Atom): Boolean = atoms(atom)

    def +(s: (Atom.Variable, Atom.Constant), p: Int, o: (Atom.Variable, Atom.Constant)): VariableMap = {
      new InjectiveMapping(addConstant(s._1, s._2, o._1, o._2), atoms + Atom(s._2, p, o._2))
    }

    def +(s: Atom.Constant, p: Int, o: (Atom.Variable, Atom.Constant)): VariableMap = {
      new InjectiveMapping(addConstant(o._1, o._2), atoms + Atom(s, p, o._2))
    }

    def +(s: (Atom.Variable, Atom.Constant), p: Int, o: Atom.Constant): VariableMap = {
      new InjectiveMapping(addConstant(s._1, s._2), atoms + Atom(s._2, p, o))
    }

    def +(s: Atom.Constant, p: Int, o: Atom.Constant): VariableMap = {
      new InjectiveMapping(hmap, atoms + Atom(s, p, o))
    }

    def injectiveMapping: Boolean = true
  }

  private class NonInjectiveMapping(protected val hmap: Array[Int]) extends VariableMap {
    def containsAtom(atom: Atom): Boolean = false

    def +(s: (Atom.Variable, Atom.Constant), p: Int, o: (Atom.Variable, Atom.Constant)): VariableMap = {
      new NonInjectiveMapping(addConstant(s._1, s._2, o._1, o._2))
    }

    def +(s: Atom.Constant, p: Int, o: (Atom.Variable, Atom.Constant)): VariableMap = {
      new NonInjectiveMapping(addConstant(o._1, o._2))
    }

    def +(s: (Atom.Variable, Atom.Constant), p: Int, o: Atom.Constant): VariableMap = {
      new NonInjectiveMapping(addConstant(s._1, s._2))
    }

    def +(s: Atom.Constant, p: Int, o: Atom.Constant): VariableMap = this

    def injectiveMapping: Boolean = false
  }

  def apply(injectiveMapping: Boolean): VariableMap = if (injectiveMapping) {
    new InjectiveMapping(Array.empty, Set.empty)
  } else {
    new NonInjectiveMapping(Array.empty)
  }

}
