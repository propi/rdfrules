package eu.easyminer.rdf.cli

import eu.easyminer.rdf.cli.impl.{ComplexState, ComplexStateInvoker}

import scala.io.StdIn

/**
  * Created by propan on 15. 4. 2017.
  */
object Main {

  def main(args: Array[String]): Unit = {
    ComplexStateInvoker((v: String) => System.out.println(v)).processInput(
      ComplexState(), {
        System.out.print("> ")
        System.out.flush()
        StdIn.readLine()
      })
  }

}
