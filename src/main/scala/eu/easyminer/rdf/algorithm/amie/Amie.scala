package eu.easyminer.rdf.algorithm.amie

import eu.easyminer.rdf.data.{HashQueue, TripleHashIndex}
import eu.easyminer.rdf.rule.RuleConstraint.OnlyPredicates
import eu.easyminer.rdf.rule._
import eu.easyminer.rdf.utils.BasicFunctions.Match
import eu.easyminer.rdf.utils.HowLong

import scala.collection.mutable.ListBuffer

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class Amie private(thresholds: Threshold.Thresholds, rulePattern: Option[RulePattern], constraints: List[RuleConstraint]) {

  import Counting._

  type SearchSpace = HashQueue[Rule[List[Atom]]]

  lazy val isWithInstances = constraints.contains(RuleConstraint.WithInstances)
  lazy val minSupport = thresholds(Threshold.MinSupport).asInstanceOf[Threshold.MinSupport].value
  lazy val maxRuleLength = thresholds(Threshold.MaxRuleLength).asInstanceOf[Threshold.MaxRuleLength].value
  lazy val minHeadCoverage = thresholds(Threshold.MinHeadCoverage).asInstanceOf[Threshold.MinHeadCoverage].value
  lazy val bodyPattern = rulePattern.map(_.antecedent.reverse).getOrElse(Nil)

  def addThreshold(threshold: Threshold) = {
    thresholds += (threshold.companion -> threshold)
    this
  }

  def addConstraint(ruleConstraint: RuleConstraint) = new Amie(thresholds, rulePattern, ruleConstraint :: constraints)

  def setRulePattern(rulePattern: RulePattern) = new Amie(thresholds, Some(rulePattern), constraints)

  private def getHeads(implicit tripleMap: TripleHashIndex.TripleMap) = {
    val atomIterator = rulePattern.map { x =>
      x.consequent.predicate
        .map(Atom(x.consequent.subject, _, x.consequent.`object`))
        .map(Iterator(_))
        .getOrElse(tripleMap.keysIterator.map(Atom(x.consequent.subject, _, x.consequent.`object`)))
    }.getOrElse(tripleMap.keysIterator.map(Atom(Atom.Variable(0), _, Atom.Variable(1))))
    if (isWithInstances) {
      atomIterator.flatMap { atom =>
        if (atom.subject.isInstanceOf[Atom.Variable] && atom.`object`.isInstanceOf[Atom.Variable]) {
          val it1 = tripleMap(atom.predicate).subjects.keysIterator.map(subject => Atom(Atom.Constant(subject), atom.predicate, atom.`object`))
          val it2 = tripleMap(atom.predicate).objects.keysIterator.map(`object` => Atom(atom.subject, atom.predicate, Atom.Constant(`object`)))
          Iterator(atom) ++ it1 ++ it2
        } else {
          Iterator(atom)
        }
      }
    } else {
      atomIterator
    }
  }

  private def filterHeadsBySupport(atoms: Iterator[Atom])(implicit tripleMap: TripleHashIndex.TripleMap) = atoms.flatMap { atom =>
    val tripleIndex = tripleMap(atom.predicate)
    Some(atom.subject, atom.`object`).collect {
      case (v1: Atom.Variable, v2: Atom.Variable) => Rule.TwoDanglings(v1, v2, Nil) -> tripleIndex.size
      case (Atom.Constant(c), v1: Atom.Variable) => Rule.OneDangling(v1, Nil) -> tripleIndex.subjects.get(c).map(_.size).getOrElse(0)
      case (v1: Atom.Variable, Atom.Constant(c)) => Rule.OneDangling(v1, Nil) -> tripleIndex.objects.get(c).map(_.size).getOrElse(0)
    }.filter(_._2 >= minSupport).map(x => DanglingRule(Nil, atom, collection.mutable.HashMap(Measure.HeadSize(x._2), Measure.Support(x._2)), x._1, x._1.danglings.max))
  }

  private def addDangling(rule: Rule[List[Atom]])(implicit tripleMap: TripleHashIndex.TripleMap) = {
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
    val extensions: List[(Atom.Variable, Rule.DanglingVariables)] = rule match {
      case ClosedRule(_, _, _, variables, _) => variables.map(_ -> Rule.OneDangling(danglingVariable, variables))
      case DanglingRule(_, _, _, variables, _) => variables match {
        case Rule.OneDangling(dangling, others) => List(dangling -> Rule.OneDangling(danglingVariable, dangling :: others))
        case Rule.TwoDanglings(dangling1, dangling2, others) => List(dangling1 -> dangling2, dangling2 -> dangling1).map(x => x._1 -> Rule.TwoDanglings(danglingVariable, x._2, x._1 :: others))
      }
    }
    tripleMap.iterator
      //pro kazde mozne rozsireni (promennou, kterou lze rozsirit), vytvor dve relace pro dany predikat s novu promennou (nova promenna muze byt na obou stranach subject i object)
      .flatMap(p => extensions.iterator.flatMap(x => Iterator(Atom(danglingVariable, p._1, x._1), Atom(x._1, p._1, danglingVariable)).map(_ -> x._2)))
      //nove relace se pripoji k aktualnimu pravidlu
      .map { r =>
      //pokud se pridava predikat, ktery se rovna posledne pouzitemu predikatu v pravidlu, potom nove pravidlo muze mit stejny support jako puvodni
      //tedy existuji duplicitni predikaty, ktere nam za danych podminek nesnizi support, tudiz ho muzeme zkopirovat z puvodniho pravidla a nemusi se pocitat znovu
      //staci brat prvni predikat z tela protoze nemuze existovat novy predikat, ktery je mensi nez aktualni predikat z tela pravidla (viz predchozi dropWhile)
      //pro novou relaci, pokud v pravidlu existuje relace se stejnym predikatem a stejnym subjektem nebo objektem, potom pravidlo nemuze mit mensi support
      //nova relace totiz bude vzdy pokryvat, alespon jednu instanci (relaci), ktera je jiz v pravidlu obsazena -- tudiz se support nesnizi
      val measures = if ((rule.head :: rule.body).exists(x => x.predicate == r._1.predicate && x.subject == r._1.subject || x.`object` == r._1.`object`)) {
        rule.measures.clone()
      } else {
        rule.measures.empty
      }
      DanglingRule(r._1 :: rule.body, rule.head, measures, r._2, danglingVariable)
    }
  }

  private def closeDangling(rule: DanglingRule) = {
    if (rule.body.nonEmpty) {
      rule.variables match {
        //pokud je jedna dangling variable a uplne nalevo pravidla (odstranuje duplicity)
        //potom vezmi vsechny ostatni promenne a nahrad dangling promennou temito promennymi
        //zaroven jsou odstranena ta pravidla, ktera obsahuji reflektujici relace nebo duplicitni relace
        //pouze nejlevější dangling promenna zajistí, ze nebudou duplicity:
        //dukaz: pokud (c, b) (a, b) -> (a, b) potom (a, b) (a, b) -> (a, b)
        //dukaz: pokud (a, c) (a, b) -> (a, b) potom (a, b) (a, b) -> (a, b) --> zbytecny krok!!!
        case Rule.OneDangling(dangling, others) if dangling == rule.body.head.subject =>
          others.iterator
            .map(x => rule.body.head.copy(subject = x))
            .filter(x => x.subject != x.`object` && !rule.body.tail.contains(x) && x != rule.head)
            .map(x => ClosedRule(x :: rule.body.tail, rule.head, rule.measures.empty, others, dangling.--))
        //pokud jsou dve dangling promenne, vezmeme opet tu nejlevejsi a nahradime ji drouhou dangling promennou
        //dukaz: pokud (c, b) -> (a, b) potom (a, b) -> (a, b)
        //dukaz: pokud (a, c) -> (a, b) potom (a, b) -> (a, b) --> zbytecny krok!!!
        //neni potreba kontrolovat, jestli je relace reflexni - nemuze nastat (v nejlevejsi relaci nemuzou byt dve dangling promenne)
        //je potreba kontrolovat duplicity relaci - muze nastat (viz predchozi priklad)
        case Rule.TwoDanglings(dangling1, dangling2, others) if dangling1 == rule.body.head.subject =>
          val r = rule.body.head.copy(subject = dangling2)
          if (!rule.body.tail.contains(r) && r != rule.head) {
            Iterator(ClosedRule(r :: rule.body.tail, rule.head, rule.measures.empty, dangling2 :: others, dangling1.--))
          } else {
            Iterator.empty
          }
        case _ => Iterator.empty
      }
    } else {
      Iterator.empty
    }
  }

  private def addInstance(rule: DanglingRule)(implicit tripleMap: TripleHashIndex.TripleMap) = {
    if (rule.body.nonEmpty) {
      countSupportInstances(rule)
    } else {
      Iterator.empty
    }
  }

  private def refine(rule: Rule[List[Atom]])(implicit tripleMap: TripleHashIndex.TripleMap): Iterator[Rule[List[Atom]]] = {
    val danglingRules = if (rule.ruleLength < maxRuleLength) addDangling(rule) else Iterator.empty
    val closedRules = rule match {
      case danglingRule: DanglingRule => closeDangling(danglingRule)
      case _ => Iterator.empty
    }
    val instanceRules = rule match {
      case danglingRule: DanglingRule if isWithInstances =>
        //pokud instancizujeme pravidlo, ktere ma delku pravidla maxRuleLength, potom budeme pracovat pouze s OneDangling pravidly
        //instancizujeme pouze nejlevejsi dangling promennou v tele - tedy pro TwoDanglings by nebylo pravidlo Closed a tudiz by nesplnovalo podminku
        if (rule.ruleLength == maxRuleLength) {
          if (danglingRule.variables.isInstanceOf[Rule.OneDangling]) addInstance(danglingRule) else Iterator.empty
        } else {
          addInstance(danglingRule)
        }
      case _ => Iterator.empty
    }
    (closedRules ++ instanceRules ++ danglingRules).filter { rule =>
      if (!rule.measures.contains(Measure.HeadCoverage)) {
        HowLong.howLong("support counting: ")(countSupport(rule))
        println(rule)
      }
      rule.measures(Measure.HeadCoverage).asInstanceOf[Measure.HeadCoverage].value >= minHeadCoverage
    }
  }

  private def amie(implicit queue: SearchSpace, tripleMap: TripleHashIndex.TripleMap /*, result: ListBuffer[ClosedRule]*/): Unit = {
    while (!queue.isEmpty) {
      val rule = queue.poll
      rule match {
        case closedRule: ClosedRule => //result += closedRule
          println(closedRule)
        case _ =>
      }
      if (rule.ruleLength <= maxRuleLength) {
        refine(rule).foreach(queue.add)
      }
    }
  }

  def mineRules(implicit tripleMap: TripleHashIndex.TripleMap) /*: Seq[ClosedRule]*/ = {
    for (constraint <- constraints) Match(constraint) {
      case OnlyPredicates(predicates) => tripleMap.keySet.iterator.filterNot(predicates.apply).foreach(tripleMap -= _)
    }
    val heads = filterHeadsBySupport(getHeads).toList.take(10)
    HowLong.howLong("mining")(heads.foreach { head =>
      implicit val queue = new HashQueue[Rule[List[Atom]]].add(head)
      //implicit val result = ListBuffer.empty[ClosedRule]
      amie
      //result.toList
    })
  }

}

object Amie {

  def apply() = {
    val defaultThresholds: collection.mutable.Map[Threshold.Key, Threshold] = collection.mutable.HashMap(
      Threshold.MinSupport -> Threshold.MinSupport(100),
      Threshold.MinHeadCoverage -> Threshold.MinHeadCoverage(0.01),
      Threshold.MaxRuleLength -> Threshold.MaxRuleLength(3)
    )
    new Amie(defaultThresholds, None, Nil)
  }

}