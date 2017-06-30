package eu.easyminer.rdf

import java.io._

import eu.easyminer.rdf.algorithm.amie.Amie
import eu.easyminer.rdf.data._
import eu.easyminer.rdf.rule.{Atom, AtomPattern, RuleConstraint, RulePattern}
import eu.easyminer.rdf.utils.HowLong

//import eu.easyminer.rdf.algorithm.Amie

/**
  * Created by propan on 15. 4. 2017.
  */
object Main extends App {

  //val cmd = "-const -minhc 0.01 -htr <participatedIn> yago2core.10kseedsSample.compressed.notypes.tsv"
  val cmd = "-const -minhc 0.01 -htr <participatedIn> -nc 1 yago2core.10kseedsSample.compressed.notypes.tsv"
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

  var i = 0

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
      triple.predicate == "<tems:description>" || triple.subject.startsWith("<id:") || triple.`object`.toStringValue.startsWith("<id:") ||
      !triple.subject.startsWith("<project:")
  }

  /*val a = RdfSource.Tsv.fromFile(new File("test.tsv")) { it =>
    it.filterNot(removeTriple)
  }
  */

  /*val a = RdfSource.Tsv.fromFile(new File("test.tsv"))(it => it.filterNot(removeTriple).flatMap(triple => Iterator(triple.subject, triple.predicate, triple.`object`.toStringValue)).toSet)
    .iterator
    .zipWithIndex
    .toMap*/

  val a = {
    val ois = new ObjectInputStream(new FileInputStream("resources"))
    try {
      ois.readObject().asInstanceOf[Map[String, Int]]
    } finally {
      ois.close()
    }
  }

  /*val oos = new ObjectOutputStream(new FileOutputStream("resources"))
  oos.writeObject(a)
  oos.close()*/

  //println(a.size)

  //println(RdfSource.Tsv.fromFile(new File("test.tsv"))(it => it.filterNot(removeTriple).size))

  /*RdfSource.Tsv.fromFile(new File("test.tsv")) { it =>
    it.filterNot(removeTriple).map(triple => CompressedTriple(a(triple.subject), a(triple.predicate), a(triple.`object`)))
  }*/

  val tripleIndex = RdfSource.Tsv.fromFile(new File("test.tsv"))(it => TripleHashIndex(it.filterNot(removeTriple).map(triple => CompressedTriple(a(triple.subject), a(triple.predicate), a(triple.`object`)))))

  //val tripleIndex = RdfSource.Tsv.fromFile(new File("test.tsv"))(it => TripleHashIndex(it.filterNot(removeTriple)))

  //println(tripleIndex.subjects.size)
  //println(tripleIndex.objects.size)

  Amie()
    .setRulePattern(RulePattern.apply(AtomPattern(Atom.Variable(0), Some(17392), Atom.Variable(1))) + AtomPattern(Atom.Variable(2), Some(17392), Atom.Variable(1)))
    //.setRulePattern(RulePattern(AtomPattern(Atom.Variable(1), None, Atom.Variable(0))) + AtomPattern(Atom.Constant("<J._R._R._Tolkien>"), Some("<influences>"), Atom.Variable(0)))
    .addConstraint(RuleConstraint.WithInstances)
    //.addConstraint(RuleConstraint.OnlyPredicates(Set("<participatedIn>", "<created>")))
    .mine(tripleIndex)

  HowLong.flushAllResults()

}

