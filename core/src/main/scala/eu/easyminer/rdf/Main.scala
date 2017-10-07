package eu.easyminer.rdf

import java.io.{File, FileInputStream, FileOutputStream}

import eu.easyminer.rdf.data.{Dataset, Graph, TripleItem}
import eu.easyminer.rdf.index._
import eu.easyminer.rdf.data.TripleSerialization._

//import eu.easyminer.rdf.algorithm.Amie

/**
  * Created by propan on 15. 4. 2017.
  */
object Main extends App {

  val graph = Graph("graf", new File("example2.ttl"))
  val dataset = Dataset(graph)

  //PrefixIndex.save(dataset, new FileOutputStream("prefixindex"))
  //PrefixIndex.loadToTraversable(new FileInputStream("prefixindex")).foreach(println)

  TripleIndex.save(dataset, new FileOutputStream("tripleindex"))
  val dataset2 = Dataset(Graph("graf", TripleIndex.loadToTraversable(new FileInputStream("tripleindex"))))

  TripleItemIndex.save(dataset2, new FileOutputStream("tripleitems"))
  implicit val a: collection.Map[TripleItem, Int] = TripleItemIndex.loadToMap(new FileInputStream("tripleitems"))

  a.foreach(println)

  println("*********************")

  dataset2.toTriples.foreach(println)

  val b = TripleHashIndex(dataset2.toTriples)
  println(b.size)

  //RuleIndex.save(Nil, new FileOutputStream("tripleindex"))


  //i=1 wd=1 rp=->(?0,280303,?1) c=0.2
  //i=1 wd=1 hc=0.05 c=0.2
  //docker run -ti --rm -e JAVA_OPTS="-Xmx12000M -Duser.country=US -Duser.language=en -Dfile.encoding=UTF-8" -v "$PWD:/app" -v "$HOME/.ivy2":/root/.ivy2 1science/sbt sbt

  //c=0.1 rp=(?1,4775,12049)^(?0,33697,?1>->(?0,14358,?1) l=4 i=1

  //val cmd = "-const -minhc 0.01 -htr <participatedIn> yago2core.10kseedsSample.compressed.notypes.tsv"
  //val cmd = "-const -minhc 0.01 -htr <participatedIn> -nc 1 yago2core.10kseedsSample.compressed.notypes.tsv"
  //val cmd = "yago2core.10kseedsSample.compressed.notypes.tsv"

  //AMIE.main(cmd.split(' '))

  println("*******************************************************")

  /*val pw = new PrintWriter(new File("esf.txt"))
  try {
    RdfSource.Trig.fromFile(new File("esf_2007_2013_czech_projects.trig")) { it =>
      for (triple <- it) pw.println(s"<${triple.subject}> <${triple.predicate}> <${triple.`object`.toStringValue}>")
    }
  } finally {
    pw.close()
  }*/

  /*var i = 0

  val prefixes = List(
    "http://data.openbudgets.eu/resource/dataset/esf-czech-projects/slice/expenditure/" -> "slice",
    "http://data.openbudgets.eu/resource/dataset/esf-czech-projects/observation/expenditure/" -> "observation",
    "http://data.openbudgets.eu/resource/dataset/esf-czech-projects/identifier/" -> "id",
    "http://data.openbudgets.eu/resource/dataset/esf-czech-projects/project/" -> "project",
    "http://data.openbudgets.eu/ontology/esf-czech-projects/" -> "o-project",
    "http://data.openbudgets.eu/resource/dataset/esf-czech-projects/registration/" -> "registration",
    "http://data.openbudgets.eu/resource/dataset/esf-czech-projects/totals" -> "totals",
    "http://data.openbudgets.eu/ontology/dsd/esf-czech-projects/" -> "dsd-project",
    "http://data.openbudgets.eu/ontology/dsd/esf-czech-projects" -> "dsd-project",
    "http://linked.opendata.cz/resource/region/" -> "region",
    "http://linked.opendata.cz/resource/business-entity/" -> "entity",
    "http://data.openbudgets.eu/resource/postal-address/" -> "address",
    "http://data.openbudgets.eu/codelist/cz-operational-programme/" -> "program",
    "http://data.openbudgets.eu/resource/dataset/esf-cz-projects/codelist/partner-types/" -> "partner-type",
    "http://data.openbudgets.eu/resource/codelist/payment-phase/" -> "payment-phase",
    "http://data.openbudgets.eu/resource/datasets/esf-czech-projects/codelist/project-statuses/" -> "statuses",
    "http://data.openbudgets.eu/resource/codelist/operation-character/" -> "operation-character",
    "http://www.w3.org/2004/02/skos/" -> "skos",
    "http://purl.org/linked-data/" -> "linked-data",
    "http://schema.org/" -> "schema",
    "http://www.w3.org/ns/" -> "ns",
    "http://ec.europa.eu/eurostat/ramon/rdfdata/nuts2008/" -> "nuts2008",
    "http://xmlns.com/foaf/0.1/" -> "foaf",
    "http://data.openbudgets.eu/ontology/dsd/dimension/" -> "dimension",
    "http://data.openbudgets.eu/ontology/dsd/measure/" -> "measure",
    "http://dbpedia.org/resource/" -> "dbpedia",
    "http://purl.org/dc/terms/" -> "tems",
    "http://data.openbudgets.eu/codelist/currency/" -> "currency",
    "http://www.w3.org/1999/02/22-rdf-syntax-ns#" -> "rdf",
    "http://data.openbudgets.eu/ontology/dsd/attribute/" -> "attribute",
    "http://linked.opendata.cz/resource/domain/mfcr/monitor/ciselniky/UcetniJednotka/" -> "ucetni-jednotka",
    "http://data.openbudgets.eu/resource/dataset/esf-czech-projects" -> "esf-czech-projects"
  )

  def findUriAndReplaceByPrefix(uri: String) = prefixes.find(x => uri.startsWith(x._1)).map(x => x._2 + ":" + uri.stripPrefix(x._1)).getOrElse(uri)

  def shortTriple(triple: Triple): Triple = {
    val `object` = triple.`object` match {
      case TripleObject.Uri(x) => TripleObject.Uri(findUriAndReplaceByPrefix(x))
      case x => x
    }
    Triple(findUriAndReplaceByPrefix(triple.subject), findUriAndReplaceByPrefix(triple.predicate), `object`)
  }

  /*val pw = new PrintWriter(new File("test.tsv"))

  RdfSource.Nt.fromFile(new File("test.nt")) { it =>
    it.map(shortTriple).foreach { triple =>
      i += 1
      pw.println(s"<${triple.subject}>\t<${triple.predicate}>\t<${triple.`object`.toStringValue}>.")
      if (i % 10000 == 0) println("zpracovano: " + i)
    }
  }

  pw.close()*/

  def removeTriple(triple: Triple) = {
    triple.subject == "<esf-czech-projects:>" || triple.`object`.toStringValue == "<esf-czech-projects:>" || triple.predicate == "<schema:description>" ||
      triple.predicate == "<tems:description>" || triple.subject.startsWith("<id:") || triple.`object`.toStringValue.startsWith("<id:") /* ||
      !triple.subject.startsWith("<project:")*/
  }

  implicit val debugger = Debugger()

  println("Input TSV file:")

  val inputFile = new File(/*StdIn.readLine()*/ "yago.tsv")

  val (tripleIndex, mapper) = {
    val a = RdfSource.Tsv.fromFile(inputFile)(it => it.flatMap(triple => Iterator(triple.subject, triple.predicate, triple.`object`.toStringValue)).toSet)
      .iterator
      .zipWithIndex
      .toMap
    val pw = new PrintWriter("resources-" + System.currentTimeMillis() + ".txt")
    try {
      for ((resource, id) <- a) {
        pw.println(s"$id: $resource")
      }
    } finally {
      pw.close()
    }
    val tripleIndex = RdfSource.Tsv.fromFile(inputFile)(it => TripleHashIndex(it.map(triple => CompressedTriple(a(triple.subject), a(triple.predicate), a(triple.`object`)))))
    (tripleIndex, a.iterator.map(_.swap).toMap)
  }

  //(?1,4775,12049)^(?0,33697,?1)->(?0,14358,?1)

  val rules = Amie()
    .addConstraint(RuleConstraint.WithInstances)
    //.addConstraint(RuleConstraint.WithoutDuplicitPredicates)
    .addThreshold(Threshold.MinConfidence(0.2))
    .addThreshold(Threshold.MaxRuleLength(3))
    //.setRulePattern(RulePattern(AtomPattern(Atom.Variable(0), Some(14358), Atom.Variable(1)))/* + AtomPattern(Atom.Variable(1), Some(260), Atom.Variable(2))*//* + AtomPattern(Atom.Variable(0), Some(33697), Atom.Variable(1))*//* + AtomPattern(Atom.Variable(1), Some(4775), Atom.Constant(2215))*/)
    .mine(tripleIndex)

  println("///////////////////////////////")

  println(rules.length)

  val os = new FileOutputStream("rules.bin")
  try {
    import RuleSerialization._
    val writer = Serializer.mapOutputStream[Rule](os)
    for (rule <- rules) writer.write(rule)
  } finally {
    os.close()
  }

  /*val (tripleIndex, a) = {
    val ois = new ObjectInputStream(new FileInputStream("resources"))
    try {
      val a = ois.readObject().asInstanceOf[Map[String, Int]]
      val tripleIndex = RdfSource.Tsv.fromFile(new File("test.tsv"))(it => TripleHashIndex(it.filterNot(removeTriple).map(triple => CompressedTriple(a(triple.subject), a(triple.predicate), a(triple.`object`)))))
      (tripleIndex, a.iterator.map(_.swap).toMap)
    } finally {
      ois.close()
    }(?c <isMarriedTo> ?a) -> (?a <hasChild> ?b)  9666: <isMarriedTo>  5881: <hasChild>
  }*/

  /*val (tripleIndex, mapper) = {
    val a = RdfSource.Tsv.fromFile(new File("yago.tsv")) { it =>
      it.flatMap(x => Iterator(x.subject, x.`object`.toStringValue, x.predicate)).toSet.iterator.zipWithIndex.toMap
    }
    val tripleIndex = RdfSource.Tsv.fromFile(new File("yago.tsv"))(it => TripleHashIndex(it.map(triple => CompressedTriple(a(triple.subject), a(triple.predicate), a(triple.`object`)))))
    (tripleIndex, a.iterator.map(_.swap).toMap)
  }*/

  //val tripleIndex = RdfSource.Tsv.fromFile(new File("test.tsv"))(it => TripleHashIndex(it.filterNot(removeTriple)))

  /*println("write command: ")

  Iterator.continually(StdIn.readLine()).takeWhile(_ != "q").foreach { command =>
    val smt: MiningTask[String] = new SimpleMiningTask(tripleIndex, mapper) with LineInputTaskParser with FileTaskResultWriter with RuleStringifier
    smt.runTask(command)
    println("write next command: ")
  }*/

  //val rules = Amie()
  //.setRulePattern(RulePattern.apply(AtomPattern(Atom.Variable(0), Some(17392), Atom.Variable(1))))
  //.setRulePattern(RulePattern(AtomPattern(Atom.Variable(1), None, Atom.Variable(0))) + AtomPattern(Atom.Constant("<J._R._R._Tolkien>"), Some("<influences>"), Atom.Variable(0)))
  //.addConstraint(RuleConstraint.WithInstances)
  //.addConstraint(RuleConstraint.WithoutDuplicitPredicates)
  //.addThreshold(Threshold.MinConfidence(0.9))
  //.addConstraint(RuleConstraint.OnlyPredicates(Set("<participatedIn>", "<created>")))
  //.mine(tripleIndex)

  HowLong.flushAllResults()

  actorSystem.terminate()

  println("finished")*/

}

