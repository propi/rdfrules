package eu.easyminer.rdf.task.impl

import java.io.PrintWriter

import eu.easyminer.rdf.data.TripleHashIndex
import eu.easyminer.rdf.task.{InputTaskParser, MiningTask, TaskResultWriter}
import eu.easyminer.rdf.utils.Debugger

/**
  * Created by Vaclav Zeman on 12. 7. 2017.
  */
class SimpleMiningTask(protected val tripleHashIndex: TripleHashIndex,
                       protected val mapper: Map[Int, String])
                      (protected implicit val debugger: Debugger) extends MiningTask[String] {

  self: InputTaskParser[String] with TaskResultWriter =>

  protected lazy val writer: PrintWriter = new PrintWriter("task-" + System.currentTimeMillis() + ".txt")

}
