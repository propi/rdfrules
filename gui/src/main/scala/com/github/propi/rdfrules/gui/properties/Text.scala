package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.HTMLInputElement

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
abstract class Text(name: String, title: String, default: String) extends Property {

  private var text: String = default

  final def getText: String = text

  @dom
  final protected def valueView: Binding[Div] = {
    <div>
      <input type="text" value={text} onkeyup={e: Event => text = e.srcElement.asInstanceOf[HTMLInputElement].value}/>
    </div>
  }

}
