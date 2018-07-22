package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
case class ChooseFileFromWorkspace(files: Constants[String],
                                   name: String = "choose-file-system-from-ws",
                                   title: String = "Choose a file from the workspace") extends Property {

  val selectedFile: Var[Option[String]] = Var(None)

  @dom
  protected def valueView: Binding[Div] = {
    <div>
      {for (file <- files) yield {
      <a onclick={_: Event => selectedFile.value = Some(file)}>
        {val x = selectedFile.bind
      if (x.contains(file)) {
        <strong>
          {file}
        </strong>
      } else {
        <span>
          {file}
        </span>
      }}
      </a>
    }}
    </div>
  }

}
