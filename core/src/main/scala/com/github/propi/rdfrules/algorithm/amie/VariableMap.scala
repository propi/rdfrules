package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.rule.{Atom, InstantiatedAtom}

//type VariableMap = Map[Atom.Variable, Atom.Constant]
//TODO check it. These lines are solution for this situation: (?a dbo:officialLanguage ?b) ^ (?a dbo:officialLanguage dbr:English_language) -> (?a dbo:language ?b)
//There can exist two identical mapped atoms - 2x (A dbo:officialLanguage English)
//For this variant it should be banned - so no mapped atom should be returned if the same atom was mapped in the previous rule refinement (it should reduce support and body size)
//We need to check it whether it is a good premision, because some ?a can have more official languages inluding English
//For this variant it should be useful: ( ?c <direction> "east" ) ^ ( ?b <train_id> ?c ) ^ ( ?b <train_id> ?a ) ⇒ ( ?a <direction> "east" )
//ABOVE It is isomorphic group and should be filteren after projections counting
//We partialy solved that by the switch allowDuplicitAtoms. It is true for bodySize counting, but false for projectionBounding and support counzing. Check it whether it is right!
sealed trait VariableMap {
  protected def hmapApply(x: Int): Int

  protected def hmapContains(x: Int): Boolean

  protected def hmapLength: Int

  final def getOrElse[T >: Atom.Constant](key: Atom.Variable, default: => T): T = {
    if (contains(key)) {
      Atom.Constant(hmapApply(key.index))
    } else {
      default
    }
  }

  final def contains(key: Atom.Variable): Boolean = key.index < hmapLength && hmapApply(key.index) != 0

  final def containsConstant(x: Int): Boolean = hmapContains(x)

  /**
    * Specify item from variableMap.
    * If the item is not included in variableMap it returns original item otherwise returns constant
    *
    * @param item item to be specified
    * @return specified item or original item if it is not included in the variable map
    */
  final def specifyItem(item: Atom.Item): Atom.Item = item match {
    case x: Atom.Variable => if (contains(x)) apply(x) else x
    case x => x
  }

  final def specifyAtom(atom: Atom): Atom = Atom(specifyItem(atom.subject), atom.predicate, specifyItem(atom.`object`))

  final def apply(key: Atom.Variable): Atom.Constant = Atom.Constant(hmapApply(key.index))

  final def get(key: Atom.Variable): Option[Atom.Constant] = if (contains(key)) Some(apply(key)) else None

  def containsAtom(atom: InstantiatedAtom): Boolean

  def +(s: (Atom.Variable, Atom.Constant), p: Int, o: (Atom.Variable, Atom.Constant)): VariableMap

  def +(s: Atom.Constant, p: Int, o: (Atom.Variable, Atom.Constant)): VariableMap

  def +(s: (Atom.Variable, Atom.Constant), p: Int, o: Atom.Constant): VariableMap

  def +(s: Atom.Constant, p: Int, o: Atom.Constant): VariableMap

  /*def enqueued[T](s: (Atom.Variable, Atom.Constant), p: Int, o: (Atom.Variable, Atom.Constant))(f: VariableMap => T): T

  def enqueued[T](s: Atom.Constant, p: Int, o: (Atom.Variable, Atom.Constant))(f: VariableMap => T): T

  def enqueued[T](s: (Atom.Variable, Atom.Constant), p: Int, o: Atom.Constant)(f: VariableMap => T): T

  def enqueued[T](s: Atom.Constant, p: Int, o: Atom.Constant)(f: VariableMap => T): T*/

  def injectiveMapping: Boolean
}

object VariableMap {

  sealed trait Immutable extends VariableMap {
    protected val hmap: Array[Int]

    //override def toString: String = hmap.iterator.zipWithIndex.filter(_._1 != 0).map(x => Atom.Variable(x._2) -> Atom.Constant(x._1)).mkString(", ")

    final protected def hmapApply(x: Int): Int = hmap(x)

    final protected def hmapContains(x: Int): Boolean = hmap.contains(x)

    final protected def hmapLength: Int = hmap.length

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

    /*def enqueued[T](s: (Atom.Variable, Atom.Constant), p: Int, o: (Atom.Variable, Atom.Constant))(f: VariableMap => T): T = {
      if (contains(s._1)) {
        enqueued(s._2, p, o)(f)
      } else if (contains(o._1)) {
        enqueued(s, p, o._2)(f)
      } else {
        f(this + (s, p, o))
      }
    }

    def enqueued[T](s: Atom.Constant, p: Int, o: (Atom.Variable, Atom.Constant))(f: VariableMap => T): T = {
      if (contains(o._1)) {
        enqueued(s, p, o._2)(f)
      } else {
        f(this + (s, p, o))
      }
    }

    def enqueued[T](s: (Atom.Variable, Atom.Constant), p: Int, o: Atom.Constant)(f: VariableMap => T): T = {
      if (contains(s._1)) {
        enqueued(s._2, p, o)(f)
      } else {
        f(this + (s, p, o))
      }
    }

    def enqueued[T](s: Atom.Constant, p: Int, o: Atom.Constant)(f: VariableMap => T): T = f(this + (s, p, o))*/
  }

  /*sealed trait Mutable extends VariableMap {
    private var hmap: Array[Int] = new Array[Int](4)

    override def toString: String = hmap.iterator.zipWithIndex.filter(_._1 != 0).map(x => Atom.Variable(x._2) -> Atom.Constant(x._1)).mkString(", ")

    final protected def hmapApply(x: Int): Int = hmap(x)

    final protected def hmapContains(x: Int): Boolean = hmap.contains(x)

    final protected def hmapLength: Int = hmap.length

    final protected def addConstant(v: Atom.Variable, c: Atom.Constant): Unit = {
      if (v.index >= hmap.length) {
        val _hmap = new Array[Int](v.index + 1)
        Array.copy(hmap, 0, _hmap, 0, hmap.length)
        hmap = _hmap
      }
      hmap.update(v.index, c.value)
    }

    final protected def -(key: Int): Unit = hmap.update(key, 0)

    protected def -(atom: InstantiatedAtom): Unit

    def enqueued[T](s: (Atom.Variable, Atom.Constant), p: Int, o: (Atom.Variable, Atom.Constant))(f: VariableMap => T): T = {
      if (contains(s._1)) {
        enqueued(s._2, p, o)(f)
      } else if (contains(o._1)) {
        enqueued(s, p, o._2)(f)
      } else {
        try {
          f(this + (s, p, o))
        } finally {
          this - s._1.index
          this - o._1.index
          this - InstantiatedAtom(s._2.value, p, o._2.value)
        }
      }
    }

    def enqueued[T](s: Atom.Constant, p: Int, o: (Atom.Variable, Atom.Constant))(f: VariableMap => T): T = {
      if (contains(o._1)) {
        enqueued(s, p, o._2)(f)
      } else {
        try {
          f(this + (s, p, o))
        } finally {
          this - o._1.index
          this - InstantiatedAtom(s.value, p, o._2.value)
        }
      }
    }

    def enqueued[T](s: (Atom.Variable, Atom.Constant), p: Int, o: Atom.Constant)(f: VariableMap => T): T = {
      if (contains(s._1)) {
        enqueued(s._2, p, o)(f)
      } else {
        try {
          f(this + (s, p, o))
        } finally {
          this - s._1.index
          this - InstantiatedAtom(s._2.value, p, o.value)
        }
      }
    }

    def enqueued[T](s: Atom.Constant, p: Int, o: Atom.Constant)(f: VariableMap => T): T = {
      if (containsAtom(InstantiatedAtom(s.value, p, o.value))) {
        f(this)
      } else {
        try {
          f(this + (s, p, o))
        } finally {
          this - InstantiatedAtom(s.value, p, o.value)
        }
      }
    }
  }

  private class MutableInjectiveMapping extends Mutable {
    private val atoms: collection.mutable.Set[InstantiatedAtom] = collection.mutable.HashSet.empty

    def containsAtom(atom: InstantiatedAtom): Boolean = atoms(atom)

    final protected def -(atom: InstantiatedAtom): Unit = atoms -= atom

    protected def +(s: (Atom.Variable, Atom.Constant), p: Int, o: (Atom.Variable, Atom.Constant)): VariableMap = {
      addConstant(s._1, s._2)
      addConstant(o._1, o._2)
      atoms += InstantiatedAtom(s._2.value, p, o._2.value)
      this
    }

    protected def +(s: Atom.Constant, p: Int, o: (Atom.Variable, Atom.Constant)): VariableMap = {
      addConstant(o._1, o._2)
      atoms += InstantiatedAtom(s.value, p, o._2.value)
      this
    }

    protected def +(s: (Atom.Variable, Atom.Constant), p: Int, o: Atom.Constant): VariableMap = {
      addConstant(s._1, s._2)
      atoms += InstantiatedAtom(s._2.value, p, o.value)
      this
    }

    protected def +(s: Atom.Constant, p: Int, o: Atom.Constant): VariableMap = {
      atoms += InstantiatedAtom(s.value, p, o.value)
      this
    }

    def injectiveMapping: Boolean = true
  }*/

  private class InjectiveMapping(protected val hmap: Array[Int], atoms: Set[InstantiatedAtom]) extends Immutable {
    def containsAtom(atom: InstantiatedAtom): Boolean = atoms(atom)

    def +(s: (Atom.Variable, Atom.Constant), p: Int, o: (Atom.Variable, Atom.Constant)): VariableMap = {
      if (contains(s._1)) {
        this + (s._2, p, o)
      } else if (contains(o._1)) {
        this + (s, p, o._2)
      } else {
        new InjectiveMapping(addConstant(s._1, s._2, o._1, o._2), atoms + InstantiatedAtom(s._2.value, p, o._2.value))
      }
    }

    def +(s: Atom.Constant, p: Int, o: (Atom.Variable, Atom.Constant)): VariableMap = {
      if (contains(o._1)) {
        this + (s, p, o._2)
      } else {
        new InjectiveMapping(addConstant(o._1, o._2), atoms + InstantiatedAtom(s.value, p, o._2.value))
      }
    }

    def +(s: (Atom.Variable, Atom.Constant), p: Int, o: Atom.Constant): VariableMap = {
      if (contains(s._1)) {
        this + (s._2, p, o)
      } else {
        new InjectiveMapping(addConstant(s._1, s._2), atoms + InstantiatedAtom(s._2.value, p, o.value))
      }
    }

    def +(s: Atom.Constant, p: Int, o: Atom.Constant): VariableMap = {
      val ia = InstantiatedAtom(s.value, p, o.value)
      if (containsAtom(ia)) {
        this
      } else {
        new InjectiveMapping(hmap, atoms + InstantiatedAtom(s.value, p, o.value))
      }
    }

    def injectiveMapping: Boolean = true
  }

  private class NonInjectiveMapping(protected val hmap: Array[Int]) extends Immutable {
    def containsAtom(atom: InstantiatedAtom): Boolean = false

    def +(s: (Atom.Variable, Atom.Constant), p: Int, o: (Atom.Variable, Atom.Constant)): VariableMap = {
      if (contains(s._1)) {
        this + (s._2, p, o)
      } else if (contains(o._1)) {
        this + (s, p, o._2)
      } else {
        new NonInjectiveMapping(addConstant(s._1, s._2, o._1, o._2))
      }
    }

    def +(s: Atom.Constant, p: Int, o: (Atom.Variable, Atom.Constant)): VariableMap = {
      if (contains(o._1)) {
        this + (s, p, o._2)
      } else {
        new NonInjectiveMapping(addConstant(o._1, o._2))
      }
    }

    def +(s: (Atom.Variable, Atom.Constant), p: Int, o: Atom.Constant): VariableMap = {
      if (contains(s._1)) {
        this + (s._2, p, o)
      } else {
        new NonInjectiveMapping(addConstant(s._1, s._2))
      }
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
