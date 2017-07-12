package eu.easyminer.rdf.algorithm.amie

import eu.easyminer.rdf.data.{HashQueue, TripleHashIndex}
import eu.easyminer.rdf.rule.RuleConstraint.OnlyPredicates
import eu.easyminer.rdf.rule._
import eu.easyminer.rdf.utils.BasicFunctions.Match
import eu.easyminer.rdf.utils.{Debugger, HowLong}

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class Amie private(thresholds: Threshold.Thresholds, rulePattern: Option[RulePattern], constraints: List[RuleConstraint])(implicit debugger: Debugger) {

  lazy val minSupport = thresholds(Threshold.MinSupport).asInstanceOf[Threshold.MinSupport].value

  def addThreshold(threshold: Threshold) = {
    thresholds += threshold
    this
  }

  def addConstraint(ruleConstraint: RuleConstraint) = new Amie(thresholds, rulePattern, ruleConstraint :: constraints)

  def setRulePattern(rulePattern: RulePattern) = new Amie(thresholds, Some(rulePattern), constraints)

  def mine(tripleIndex: TripleHashIndex) = {
    for (constraint <- constraints) Match(constraint) {
      case OnlyPredicates(predicates) => tripleIndex.predicates.keySet.iterator.filterNot(predicates.apply).foreach(tripleIndex.predicates -= _)
    }
    val rules = {
      val process = new AmieProcess(tripleIndex)
      val heads = process.filterHeadsBySupport(process.getHeads).toList
      debugger.debug("Amie rules mining", heads.length) { ad =>
        heads.par.flatMap { head =>
          ad.result()(process.searchRules(head))
        }
      }
    }
    val confidenceCounting = new AmieConfidenceCounting(tripleIndex)
    debugger.debug("Rules confidence counting", rules.size) { ad =>
      rules.filter { rule =>
        ad.result()(confidenceCounting.filterByConfidence(rule))
      }
    }.toList
  }

  private class AmieConfidenceCounting(val tripleIndex: TripleHashIndex) extends RuleCounting {
    val minConfidence = thresholds.getOrElse(Threshold.MinConfidence, Threshold.MinConfidence(0.0)).asInstanceOf[Threshold.MinConfidence].value

    def filterByConfidence(rule: ClosedRule) = {
      rule.withConfidence.measures(Measure.Confidence).asInstanceOf[Measure.Confidence].value >= minConfidence
    }

  }

  private class AmieProcess(val tripleIndex: TripleHashIndex) extends RuleExpansion with AtomCounting {

    val isWithInstances: Boolean = constraints.contains(RuleConstraint.WithInstances)
    val minHeadCoverage: Double = thresholds(Threshold.MinHeadCoverage).asInstanceOf[Threshold.MinHeadCoverage].value
    val maxRuleLength: Int = thresholds(Threshold.MaxRuleLength).asInstanceOf[Threshold.MaxRuleLength].value
    val bodyPattern: IndexedSeq[AtomPattern] = rulePattern.map(_.antecedent).getOrElse(IndexedSeq.empty)
    val withDuplicitPredicates: Boolean = !constraints.contains(RuleConstraint.WithoutDuplicitPredicates)

    def getHeads = {
      val atomIterator = rulePattern.map { x =>
        x.consequent.predicate
          .map(Atom(x.consequent.subject, _, x.consequent.`object`))
          .map(Iterator(_))
          .getOrElse(tripleIndex.predicates.keysIterator.map(Atom(x.consequent.subject, _, x.consequent.`object`)))
      }.getOrElse(tripleIndex.predicates.keysIterator.map(Atom(Atom.Variable(0), _, Atom.Variable(1))))
      if (isWithInstances) {
        atomIterator.flatMap { atom =>
          if (atom.subject.isInstanceOf[Atom.Variable] && atom.`object`.isInstanceOf[Atom.Variable]) {
            val it1 = tripleIndex.predicates(atom.predicate).subjects.keysIterator.map(subject => Atom(Atom.Constant(subject), atom.predicate, atom.`object`))
            val it2 = tripleIndex.predicates(atom.predicate).objects.keysIterator.map(`object` => Atom(atom.subject, atom.predicate, Atom.Constant(`object`)))
            Iterator(atom) ++ it1 ++ it2
          } else {
            Iterator(atom)
          }
        }
      } else {
        atomIterator
      }
    }

    def filterHeadsBySupport(atoms: Iterator[Atom]) = atoms.flatMap { atom =>
      val tm = tripleIndex.predicates(atom.predicate)
      Some(atom.subject, atom.`object`).collect {
        case (v1: Atom.Variable, v2: Atom.Variable) => Rule.TwoDanglings(v1, v2, Nil) -> tm.size
        case (Atom.Constant(c), v1: Atom.Variable) => Rule.OneDangling(v1, Nil) -> tm.subjects.get(c).map(_.size).getOrElse(0)
        case (v1: Atom.Variable, Atom.Constant(c)) => Rule.OneDangling(v1, Nil) -> tm.objects.get(c).map(_.size).getOrElse(0)
      }.filter(_._2 >= minSupport).map(x => DanglingRule(Vector.empty, atom)(collection.mutable.HashMap(Measure.HeadSize(x._2), Measure.Support(x._2)), x._1, x._1.danglings.max, getAtomTriples(atom).toIndexedSeq))
    }

    def searchRules(initRule: Rule) = {
      val queue = new HashQueue[Rule].add(initRule)
      val result = collection.mutable.ListBuffer.empty[ClosedRule]
      while (!queue.isEmpty) {
        //if (queue.size % 500 == 0) println("queue size (" + Thread.currentThread().getName + "): " + queue.size)
        val rule = queue.poll
        rule match {
          case closedRule: ClosedRule => result += closedRule
          case _ =>
        }
        if (rule.ruleLength < maxRuleLength) {
          for (rule <- rule.expand(Debugger.EmptyDebugger)) queue.add(rule)
        }
      }
      result.toList
    }

  }

  /*private def addDangling(rule: Rule[List[Atom]])(implicit tripleMap: TripleHashIndex.TripleMap) = {
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
      val measures = if ((rule.head :: rule.body).exists(x => x.predicate == r._1.predicate && (x.subject == r._1.subject || x.`object` == r._1.`object`))) {
        rule.measures.clone()
      } else {
        rule.measures.empty
      }
      DanglingRule(r._1 :: rule.body, rule.head, measures, r._2, danglingVariable)
    }
  }*/

  /*private def closeDangling(rule: DanglingRule) = {
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
  }*/

  /*private def addInstance(rule: DanglingRule)(implicit tripleMap: TripleHashIndex.TripleMap) = {
    if (rule.body.nonEmpty) {
      countSupportInstances(rule)
    } else {
      Iterator.empty
    }
  }*/

  /*private def refine(rule: Rule[List[Atom]])(implicit tripleMap: TripleHashIndex.TripleMap): Iterator[Rule[List[Atom]]] = {
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
  }*/

  /*private def amie(implicit queue: SearchSpace, tripleMap: TripleHashIndex.TripleMap /*, result: ListBuffer[ClosedRule]*/): Unit = {
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
  }*/

  /*def mineRules(implicit tripleMap: TripleHashIndex.TripleMap) /*: Seq[ClosedRule]*/ = {
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
  }*/

}

object Amie {

  def apply()(implicit debugger: Debugger) = {
    val defaultThresholds: collection.mutable.Map[Threshold.Key, Threshold] = collection.mutable.HashMap(
      Threshold.MinSupport -> Threshold.MinSupport(100),
      Threshold.MinHeadCoverage -> Threshold.MinHeadCoverage(0.01),
      Threshold.MaxRuleLength -> Threshold.MaxRuleLength(3)
    )
    new Amie(defaultThresholds, None, Nil)
  }

}