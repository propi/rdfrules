package eu.easyminer.rdf

import java.io.File

import amie.mining.AMIE
//import eu.easyminer.rdf.algorithm.Amie

import scala.io.Source

/**
  * Created by propan on 15. 4. 2017.
  */
object Main extends App {

  val cmd = "-const -minhc 0.01 -htr <directed> yago2core.10kseedsSample.compressed.notypes.tsv"
  //val cmd = "yago2core.10kseedsSample.compressed.notypes.tsv"

  AMIE.main(cmd.split(' '))

  println("*******************************************************")

  //Amie.mine(new File("yago2core.10kseedsSample.compressed.notypes.tsv"))

}

