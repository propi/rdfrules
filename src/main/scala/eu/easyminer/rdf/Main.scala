package eu.easyminer.rdf

import java.io.File

import amie.mining.AMIE
import eu.easyminer.rdf.algorithm.amie.Amie
import eu.easyminer.rdf.data.{TripleHashIndex, Tsv2Rdf}
import eu.easyminer.rdf.rule.{Atom, AtomPattern, RuleConstraint, RulePattern}
import eu.easyminer.rdf.utils.HowLong

//import eu.easyminer.rdf.algorithm.Amie

import scala.io.Source

/**
  * Created by propan on 15. 4. 2017.
  */
object Main extends App {

  //val cmd = "-const -minhc 0.01 -htr <participatedIn> yago2core.10kseedsSample.compressed.notypes.tsv"
  val cmd = "-minhc 0.01 -htr <participatedIn> -nc 1 yago2core.10kseedsSample.compressed.notypes.tsv"
  //val cmd = "yago2core.10kseedsSample.compressed.notypes.tsv"

  //AMIE.main(cmd.split(' '))

  println("*******************************************************")

  Amie()
    .setRulePattern(RulePattern.apply(AtomPattern(Atom.Variable(0), Some("<participatedIn>"), Atom.Variable(1))))
    //.addConstraint(RuleConstraint.OnlyPredicates(Set("<participatedIn>", "<created>")))
    .mineRules(Tsv2Rdf(new File("yago2core.10kseedsSample.compressed.notypes.tsv"))(TripleHashIndex.apply))

  HowLong.flushAllResults()

}

