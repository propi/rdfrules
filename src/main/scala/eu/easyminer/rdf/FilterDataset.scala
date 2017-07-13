package eu.easyminer.rdf

import java.io.{File, PrintWriter}

import eu.easyminer.rdf.data.{RdfSource, Triple}

import scala.io.StdIn

/**
  * Created by Vaclav Zeman on 12. 7. 2017.
  */
object FilterDataset extends App {

  println("Input TSV file:")
  val inputFile = new File(StdIn.readLine())

  println("Output TSV file:")
  val outputFile = new File(StdIn.readLine())

  def removeTriple(triple: Triple) = {
    triple.subject == "<esf-czech-projects:>" || triple.`object`.toStringValue == "<esf-czech-projects:>" || triple.predicate == "<schema:description>" ||
      triple.predicate == "<tems:description>" || triple.subject.startsWith("<id:") || triple.`object`.toStringValue.startsWith("<id:") ||
      triple.subject.startsWith("<project:") && triple.predicate == "<foaf:name>"
  }

  val pw = new PrintWriter(outputFile)
  try {
    RdfSource.Tsv.fromFile(inputFile)(it => it.filterNot(removeTriple).foreach(triple => pw.println(s"${triple.subject}\t${triple.predicate}\t${triple.`object`.toStringValue}.")))
  } finally {
    pw.close()
  }

}
