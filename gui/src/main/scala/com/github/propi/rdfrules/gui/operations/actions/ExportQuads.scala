package com.github.propi.rdfrules.gui.operations.actions

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.results.NoResult
import com.github.propi.rdfrules.gui.utils.CommonValidators.RegExp
import com.github.propi.rdfrules.gui._
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.concurrent.Future

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class ExportQuads(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.ExportQuads
  val properties: Constants[Property] = Constants(
    new ChooseFileFromWorkspace(Workspace.loadFiles, true, "path", "Path", validator = RegExp(".+[.](ttl|nt|nq|trig|trix|tsv|cache)([.](gz|bz2))?$"), "file")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def buildActionProgress(id: Future[String]): Option[ActionProgress] = Some(new NoResult(info.title, id))
}