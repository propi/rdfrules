package eu.easyminer.rdf.algorithm

import java.io.File

import eu.easyminer.rdf.data.{Triple, TripleHashIndex}
import eu.easyminer.rdf.utils.HowLong._

import scala.language.implicitConversions

/**
  * Created by propan on 16. 4. 2017.
  */
/*object Amie {

  type TripleMap = collection.Map[String, TripleIndex]
  type Measures = collection.mutable.HashMap[MeasureKey, Measure]
  type SearchSpace = Stack[Iterator[Rule]]

  val minSupport = 100
  val minHC = 0.01
  val maxRuleLength = 3

  class TripleIndex(val subjects: collection.Map[String, collection.Set[String]], val objects: collection.Map[String, collection.Set[String]])

  sealed trait AtomItem

  case class Variable(index: Int) extends AtomItem {
    def value = "?" + Iterator.iterate(math.floor(index.toDouble / 26) -> (index.toDouble % 26))(x => math.floor(x._1 / 26) -> ((x._1 % 26) - 1))
      .takeWhile(_._2 >= 0)
      .map(x => (97 + x._2).toChar)
      .foldLeft("")((x, y) => y + x)

    def ++ = Variable(index + 1)

    def -- = Variable(index - 1)

    override def toString: String = value
  }

  case class Constant(value: String) extends AtomItem

  case class Relation(subject: AtomItem, predicate: String, `object`: AtomItem) {
    override def toString: String = s"<$subject $predicate ${`object`}>"
  }

  sealed trait Measure

  sealed trait MeasureKey

  object Measure {

    case class Support(value: Int) extends Measure

    object Support extends MeasureKey

    case class HeadCoverage(value: Double) extends Measure

    object HeadCoverage extends MeasureKey

    case class HeadSize(value: Int) extends Measure

    object HeadSize extends MeasureKey

    case class BodySize(value: Int) extends Measure

    object BodySize extends MeasureKey

    case class Confidence(value: Double) extends Measure

    object Confidence extends MeasureKey

    case class PcaBodySize(value: Int) extends Measure

    object PcaBodySize extends MeasureKey

    case class PcaConfidence(value: Double) extends Measure

    object PcaConfidence extends MeasureKey

    case class MaxVariable(variable: Variable) extends Measure

    object MaxVariable extends MeasureKey

    implicit def measureToMeasureWithKey(measure: Measure): (MeasureKey, Measure) = {
      val key = measure match {
        case _: Support => Support
        case _: HeadCoverage => HeadCoverage
        case _: HeadSize => HeadSize
        case _: BodySize => BodySize
        case _: Confidence => Confidence
        case _: PcaBodySize => PcaBodySize
        case _: PcaConfidence => PcaConfidence
        case _: MaxVariable => MaxVariable
      }
      key -> measure
    }

  }

  trait Rule {
    val body: List[Relation]
    val head: Relation
    val measures: Measures

    lazy val maxVariable = measures.get(Measure.MaxVariable).map(_.asInstanceOf[Measure.MaxVariable].variable).getOrElse {
      (head :: body).iterator.flatMap(x => List(x.subject, x.`object`)).collect({ case x: Variable => x }).maxBy(_.index)
    }

    def ruleLength = body.length + 1

    override def toString: String = body.mkString(" ^ ") + " -> " + head + "  :  " + " support:" + measures(Measure.Support).asInstanceOf[Measure.Support].value + ", hc:" + measures(Measure.HeadCoverage).asInstanceOf[Measure.HeadCoverage].value
  }

  sealed trait DanglingVariables {
    def others: List[Variable]

    def danglings: List[Variable]
  }

  case class OneDangling(dangling: Variable, others: List[Variable]) extends DanglingVariables {
    def danglings: List[Variable] = List(dangling)
  }

  case class TwoDanglings(dangling1: Variable, dangling2: Variable, others: List[Variable]) extends DanglingVariables {
    def danglings: List[Variable] = List(dangling1, dangling2)
  }

  case class ClosedRule(body: List[Relation], head: Relation, measures: Measures, variables: List[Variable]) extends Rule {
    //lazy val variables = (head :: body).iterator.flatMap(x => List(x.subject, x.`object`)).collect { case x: Variable => x }.toSet
  }

  case class DanglingRule(body: List[Relation], head: Relation, measures: Measures, variables: DanglingVariables) extends Rule {
    /*lazy val openVariables = (head :: body).iterator.flatMap(x => List(x.subject, x.`object`)).collect { case x: Variable => x }.foldLeft(Set.empty[Variable]) { (ov, v) =>
      if (ov(v)) ov - v else ov + v
    }*/
  }

  /*private def countRelation(relation: Relation)(implicit triples: List[Triple]) = {
    val cf: Triple => Boolean = relation match {
      case Relation(_: Variable, _, _: Variable) => triple => true
      case Relation(_: Variable, _, Constant(x)) => triple => triple.`object` == x
      case Relation(Constant(x), _, _: Variable) => triple => triple.subject == x
      case Relation(Constant(x), _, Constant(y)) => triple => triple.subject == x && triple.`object` == y
    }
    triples.count(triple => triple.predicate == relation.predicate && cf(triple))
  }*/

  /*private def replaceVariable(item: AtomItem, replaceRule: Option[(Variable, Constant)]) = (item, replaceRule) match {
    case (Variable(x), Some((Variable(y), c))) if x == y => c
    case _ => item
  }

  private def replaceVariables(relations: List[Relation], subject: Option[(Variable, Constant)], `object`: Option[(Variable, Constant)]) = relations.map { relation =>
    Relation(replaceVariable(relation.subject, subject), relation.predicate, replaceVariable(relation.`object`, `object`))
  }*/

  @scala.annotation.tailrec
  private def sortRelations(relations: List[Relation], specifiedVariables: Set[Variable], result: List[Relation] = Nil): List[Relation] = {
    def count(r: Relation) = specifiedVariables.count(y => y == r.subject || y == r.`object`)
    if (relations.length > 1) {
      val (_, head, tail) = relations.tail.foldLeft(count(relations.head), relations.head, List.empty[Relation]) {
        case ((bestMatches, best, rest), relation) if bestMatches < 2 =>
          val matches = count(relation)
          if (matches > bestMatches) {
            (matches, relation, best :: rest)
          } else {
            (bestMatches, best, relation :: rest)
          }
        case ((bestMatches, best, rest), relation) => (bestMatches, best, relation :: rest)
      }
      sortRelations(tail, specifiedVariables ++ List(head.subject, head.`object`).collect({ case x: Variable => x }), head :: result)
    } else {
      result ::: relations
    }
  }

  private def countPaths(relations: List[Relation], variableMap: Map[AtomItem, Constant])(implicit tripleMap: TripleMap, reduce: Iterator[Int] => Int): Int = relations match {
    case head :: tail =>
      val tm = tripleMap(head.predicate)
      val it = (variableMap.getOrElse(head.subject, head.subject), variableMap.getOrElse(head.`object`, head.`object`)) match {
        case (sv: Variable, ov: Variable) =>
          tm.subjects.iterator
            .flatMap(x => x._2.iterator.map(y => Triple(x._1, head.predicate, y)))
            .map(x => countPaths(tail, variableMap +(sv -> Constant(x.subject), ov -> Constant(x.`object`))))
        case (sv: Variable, Constant(oc)) =>
          tm.objects.getOrElse(oc, Set.empty[String]).iterator.map(Triple(_, head.predicate, oc)).map(x => countPaths(tail, variableMap + (sv -> Constant(x.subject))))
        case (Constant(sc), ov: Variable) =>
          tm.subjects.getOrElse(sc, Set.empty[String]).iterator.map(Triple(sc, head.predicate, _)).map(x => countPaths(tail, variableMap + (ov -> Constant(x.`object`))))
        case (Constant(sc), Constant(oc)) =>
          if (tm.subjects.getOrElse(sc, Set.empty[String]).contains(oc)) Iterator(countPaths(tail, variableMap)) else Iterator()
      }
      reduce(it)
    case Nil => 1
  }

  private def danglingInstances(relations: List[Relation], variableMap: Map[AtomItem, Constant])(implicit tripleMap: TripleMap, result: collection.mutable.Set[String] = collection.mutable.HashSet.empty): collection.mutable.Set[String] = {
    relations match {
      case head :: tail =>
        val tm = tripleMap(head.predicate)
        (variableMap.getOrElse(head.subject, head.subject), variableMap.getOrElse(head.`object`, head.`object`)) match {
          case (sv: Variable, ov: Variable) =>
            tm.subjects.iterator
              .flatMap(x => x._2.iterator.map(y => Triple(x._1, head.predicate, y)))
              .foreach(x => danglingInstances(tail, variableMap +(sv -> Constant(x.subject), ov -> Constant(x.`object`))))
          case (sv: Variable, Constant(oc)) =>
            if (tail.isEmpty) {
              tm.objects.get(oc).foreach(result ++= _)
            } else {
              tm.objects.getOrElse(oc, Set.empty[String]).iterator.map(Triple(_, head.predicate, oc)).foreach(x => danglingInstances(tail, variableMap + (sv -> Constant(x.subject))))
            }
          case (Constant(sc), ov: Variable) =>
            if (tail.isEmpty) {
              tm.subjects.get(sc).foreach(result ++= _)
            } else {
              tm.subjects.getOrElse(sc, Set.empty[String]).iterator.map(Triple(sc, head.predicate, _)).foreach(x => danglingInstances(tail, variableMap + (ov -> Constant(x.`object`))))
            }
          case (Constant(sc), Constant(oc)) =>
            if (tm.subjects.getOrElse(sc, Set.empty[String]).contains(oc)) danglingInstances(tail, variableMap)
        }
      case Nil => throw new IllegalArgumentException
    }
    result
  }

  private def countSupportInstances(rule: DanglingRule)(implicit tripleMap: TripleMap) = {
    val dangling = rule.variables.danglings.head
    if (dangling == rule.body.head.subject || dangling == rule.body.head.`object`) {
      val tm = tripleMap(rule.head.predicate)
      val sortedRelations = sortRelations(rule.body.tail, Set(rule.head.subject, rule.head.`object`).collect { case x: Variable => x }) :+ rule.body.head
      val m = collection.mutable.HashMap.empty[String, Int]
      val r = Some(rule.head).collect {
        case Relation(sv: Variable, _, ov: Variable) =>
          tm.subjects.iterator.flatMap(x => x._2.iterator.map(y => x._1 -> y)).map(x => danglingInstances(sortedRelations, Map(sv -> Constant(x._1), ov -> Constant(x._2))))
        case Relation(sv: Variable, _, Constant(oc)) =>
          val subjects = tm.objects.getOrElse(oc, Set.empty[String])
          subjects.iterator.map(_ -> oc).map(x => danglingInstances(sortedRelations, Map(sv -> Constant(x._1))))
        case Relation(Constant(sc), _, ov: Variable) =>
          val objects = tm.subjects.getOrElse(sc, Set.empty[String])
          objects.iterator.map(sc -> _).map(x => danglingInstances(sortedRelations, Map(ov -> Constant(x._2))))
      }
      for (x <- r; y <- x; i <- y) {
        m += i -> (m.getOrElse(i, 0) + 1)
      }
      val nr = if (rule.body.head.subject == dangling) (x: String) => rule.body.head.copy(subject = Constant(x)) else (x: String) => rule.body.head.copy(`object` = Constant(x))
      val nrule: (String) => Rule = rule.variables match {
        case OneDangling(_, others) => x => ClosedRule(nr(x) :: rule.body.tail, rule.head, rule.measures.empty, others)
        case TwoDanglings(_, dangling2, others) => x => DanglingRule(nr(x) :: rule.body.tail, rule.head, rule.measures.empty, OneDangling(dangling2, others))
      }
      val headSize = rule.measures(Measure.HeadSize).asInstanceOf[Measure.HeadSize]
      m.iterator.map { x =>
        val irule = nrule(x._1)
        irule.measures += Measure.MaxVariable(rule.maxVariable.--)
        irule.measures += headSize
        irule.measures += Measure.Support(x._2)
        irule.measures += Measure.HeadCoverage(x._2.toDouble / headSize.value)
        irule
      }
    } else {
      Iterator()
    }
  }

  private def countSupport(rule: Rule)(implicit tripleMap: TripleMap): Unit = {
    implicit def reduce(it: Iterator[Int]): Int = it.find(_ == 1).getOrElse(0)
    val tm = tripleMap(rule.head.predicate)
    val sortedRelations = sortRelations(rule.body, Set(rule.head.subject, rule.head.`object`).collect { case x: Variable => x })
    val (size, supp) = Some(rule.head).collect {
      case Relation(sv: Variable, _, ov: Variable) =>
        tm.subjects.iterator.flatMap(x => x._2.iterator.map(y => x._1 -> y)).map(x => 1 -> countPaths(sortedRelations, Map(sv -> Constant(x._1), ov -> Constant(x._2))))
      case Relation(sv: Variable, _, Constant(oc)) =>
        val subjects = tm.objects.getOrElse(oc, Set.empty[String])
        subjects.iterator.map(_ -> oc).map(x => 1 -> countPaths(sortedRelations, Map(sv -> Constant(x._1))))
      case Relation(Constant(sc), _, ov: Variable) =>
        val objects = tm.subjects.getOrElse(sc, Set.empty[String])
        objects.iterator.map(sc -> _).map(x => 1 -> countPaths(sortedRelations, Map(ov -> Constant(x._2))))
    }.map(_.reduce((x, y) => (x._1 + y._1, x._2 + y._2))).getOrElse(0 -> 0)
    rule.measures += Measure.Support(supp)
    rule.measures += Measure.HeadSize(size)
    rule.measures += Measure.HeadCoverage(if (size > 0) supp.toDouble / size else 0)
  }

  private def countConfidence(rule: Rule)(implicit tripleMap: TripleMap): Unit = {
    implicit def reduce(it: Iterator[Int]): Int = it.sum
    val support = rule.measures(Measure.Support).asInstanceOf[Measure.Support]
    val bodySize = countPaths(rule.body, Map.empty)
    rule.measures ++= List(Measure.BodySize(bodySize), Measure.Confidence(if (bodySize > 0) support.value.toDouble / bodySize else 0))
  }

  private def countPcaConfidence(rule: Rule)(implicit tripleMap: TripleMap): Unit = {
    implicit def reduce(it: Iterator[Int]): Int = it.sum
    val support = rule.measures(Measure.Support).asInstanceOf[Measure.Support]
    val pcaBodySize = tripleMap(rule.head.predicate).subjects.keysIterator.map(x => countPaths(rule.body, Map(rule.head.subject -> Constant(x)))).sum
    rule.measures ++= List(Measure.PcaBodySize(pcaBodySize), Measure.PcaConfidence(if (pcaBodySize > 0) support.value.toDouble / pcaBodySize else 0))
  }

  private def addDangling(rule: Rule)(implicit tripleMap: TripleMap, stack: SearchSpace) = {
    //pridava vzdy novou promennou
    val danglingVariable = rule.maxVariable.++
    //vraci sadu promennych, ktere lze rozsirit
    //rozsirit lze pouze dangling atoms, pokud pravidlo neni uzavrene a nebo vsechny atomy pokud je uzavrene
    //pokud je closed: kazdou promennou rozsir - dangling variable bude pouze jedna
    //ex: (?a ?p1 ?b) => (?a ?p2 ?b)
    //pokud je dangling: existuji celkem dve varianty - bud je pouze jedna dangling promenna nebo dve
    //pokud jedna dangling promenna: vysledkem je pravidlo opet s jednou novou dangling promennou (stara prestava byt dangling)
    //ex: (?c ?p3 ?b) ^ (?a ?p1 ?b) => (?a ?p2 ?b)
    //pokud jsou dve dangling promenne: vysledkem je pravidlo opet s dvema dangling promennymi. vraci dve varianty: prvni dangling pouzit nebo druhy pouzit
    //ex: => (?a ?p2 ?b) nebo (?c ?p1 ?b) => (?a ?p2 ?b)
    val extensions: List[(Variable, DanglingVariables)] = rule match {
      case ClosedRule(_, _, _, variables) => variables.map(_ -> OneDangling(danglingVariable, variables))
      case DanglingRule(_, _, _, variables) => variables match {
        case OneDangling(dangling, others) => List(dangling -> OneDangling(danglingVariable, dangling :: others))
        case TwoDanglings(dangling1, dangling2, others) => List(dangling1 -> dangling2, dangling2 -> dangling1).map(x => x._1 -> TwoDanglings(danglingVariable, x._2, x._1 :: others))
      }
    }
    val it = tripleMap.iterator
      //toto zajisti, ze nebudou generovany duplicity
      //novy predikat muze byt pouze vetsi nebo rovny (z tripleMapu) aktualnimu predikatu v tele pravidla
      .dropWhile(x => rule.body.headOption.exists(_.predicate != x._1))
      //pro kazde mozne rozsireni (promennou, kterou lze rozsirit), vytvor dve relace pro dany predikat s novu promennou (nova promenna muze byt na obou stranach subject i object)
      .flatMap(p => extensions.iterator.flatMap(x => Iterator(Relation(danglingVariable, p._1, x._1), Relation(x._1, p._1, danglingVariable)).map(_ -> x._2)))
      //nove relace se pripoji k aktualnimu pravidlu
      .map { r =>
      //pokud se pridava predikat, ktery se rovna posledne pouzitemu predikatu v pravidlu, potom nove pravidlo muze mit stejny support jako puvodni
      //tedy existuji duplicitni predikaty, ktere nam za danych podminek nesnizi support, tudiz ho muzeme zkopirovat z puvodniho pravidla a nemusi se pocitat znovu
      //staci brat prvni predikat z tela protoze nemuze existovat novy predikat, ktery je mensi nez aktualni predikat z tela pravidla (viz predchozi dropWhile)
      //pro novou relaci, pokud v pravidlu existuje relace se stejnym predikatem a stejnym subjektem nebo objektem, potom pravidlo nemuze mit mensi support
      //nova relace totiz bude vzdy pokryvat, alespon jednu instanci (relaci), ktera je jiz v pravidlu obsazena -- tudiz se support nesnizi
      val measures = if ((rule.body.headOption.exists(_.predicate == r._1.predicate) || rule.head.predicate == r._1.predicate)
        && (rule.head :: rule.body).exists(x => x.predicate == r._1.predicate && x.subject == r._1.subject || x.`object` == r._1.`object`)) {
        rule.measures.clone()
      } else {
        rule.measures.empty
      }
      DanglingRule(r._1 :: rule.body, rule.head, measures += Measure.MaxVariable(danglingVariable), r._2)
    }
    stack.push(it)
  }

  private def addClose(rule: Rule)(implicit stack: SearchSpace) = {
    rule match {
      //lze uzavrit pouze dangling rules, ktere maji telo
      case DanglingRule(body, head, measures, variables) if body.nonEmpty =>
        val it = variables match {
          //pokud je jedna dangling variable a uplne nalevo pravidla (odstranuje duplicity)
          //potom vezmi vsechny ostatni promenne a nahrad dangling promennou temito promennymi
          //zaroven jsou odstranena ta pravidla, ktera obsahuji reflektujici relace nebo duplicitni relace
          //pouze nejlevější dangling promenna zajistí, ze nebudou duplicity:
          //dukaz: pokud (c, b) (a, b) -> (a, b) potom (a, b) (a, b) -> (a, b)
          //dukaz: pokud (a, c) (a, b) -> (a, b) potom (a, b) (a, b) -> (a, b) --> zbytecny krok!!!
          case OneDangling(dangling, others) if dangling == body.head.subject =>
            others.iterator
              .map(x => body.head.copy(subject = x))
              .filter(x => x.subject != x.`object` && !body.tail.contains(x) && x != head)
              .map(x => ClosedRule(x :: body.tail, head, measures.empty += Measure.MaxVariable(dangling.--), others))
          //pokud jsou dve dangling promenne, vezmeme opet tu nejlevejsi a nahradime ji drouhou dangling promennou
          //dukaz: pokud (c, b) -> (a, b) potom (a, b) -> (a, b)
          //dukaz: pokud (a, c) -> (a, b) potom (a, b) -> (a, b) --> zbytecny krok!!!
          //neni potreba kontrolovat, jestli je relace reflexni - nemuze nastat (v nejlevejsi relaci nemuzou byt dve dangling promenne)
          //je potreba kontrolovat duplicity relaci - muze nastat (viz predchozi priklad)
          case TwoDanglings(dangling1, dangling2, others) if dangling1 == body.head.subject =>
            val r = body.head.copy(subject = dangling2)
            if (!body.tail.contains(r) && r != head) {
              Iterator(ClosedRule(r :: body.tail, head, measures.empty += Measure.MaxVariable(dangling1.--), dangling2 :: others))
            } else {
              Iterator.empty
            }
          case _ => Iterator.empty
        }
        if (it.hasNext) stack.push(it)
      case _ =>
    }
  }

  private def addInstance(rule: Rule)(implicit tripleMap: TripleMap, stack: SearchSpace) = {
    rule match {
      case dr: DanglingRule if rule.body.nonEmpty =>
        val it = countSupportInstances(dr)
        if (it.hasNext) stack.push(it)
      case _ =>
    }
  }

  //val test = collection.mutable.HashSet.empty[String]

  private def refine()(implicit stack: SearchSpace, tripleMap: TripleMap): Unit = {
    val it = stack.peek
    if (it.hasNext) {
      val rule = it.next()
      if (rule.ruleLength <= maxRuleLength) {
        if (!rule.measures.contains(Measure.HeadCoverage)) countSupport(rule)
        //if (test(rule.toString)) println("crash: " + rule) else test += rule.toString
        if (rule.measures(Measure.HeadCoverage).asInstanceOf[Measure.HeadCoverage].value >= minHC) {
          if (rule.isInstanceOf[ClosedRule]) {
            println(rule.getClass.getSimpleName + ": " + rule)
          }
          if (rule.ruleLength < maxRuleLength) addDangling(rule)
          addClose(rule)
          if (rule.ruleLength == maxRuleLength) {
            rule match {
              case DanglingRule(_, _, _, _: OneDangling) => addInstance(rule)
              case _ =>
            }
          } else {
            addInstance(rule)
          }
        }
      }
    } else {
      stack.pop
    }
  }

  private def searchRules()(implicit tripleMap: TripleMap) = {
    val heads = tripleMap.iterator
      .map(x => DanglingRule(Nil, Relation(Variable(0), x._1, Variable(1)), collection.mutable.HashMap(Measure.Support(math.max(x._2.subjects.size, x._2.objects.size))), TwoDanglings(Variable(0), Variable(1), Nil)))
      .filter(_.measures(Measure.Support).asInstanceOf[Measure.Support].value >= minSupport)
      .toList
      .take(1)
    heads.par.foreach {
      rule =>
        implicit val stack: SearchSpace = new Stack(Iterator(rule))
        while (!stack.isEmpty) refine()
    }
    //println(test.size)
  }

  def mine(file: File) = {
    implicit val tripleMap = Tsv2Rdf(file)(TripleHashIndex.apply)
    /*val rule = Rule(List(Relation(Variable(2), "<created>", Variable(3)), Relation(Variable(2), "<directed>", Variable(1))), Relation(Variable(0), "<directed>", Variable(1)), collection.mutable.HashMap.empty)
    howLong("suop")(countSupport(rule))
    println(rule)*/
    searchRules()
  }

}

class Stack[T](a: T*) {

  private var list: List[T] = a.toList

  def peek = list.head

  def pop = {
    val head = peek
    list = list.tail
    head
  }

  def isEmpty: Boolean = list.isEmpty

  def push(v: T) = list = v :: list
}*/