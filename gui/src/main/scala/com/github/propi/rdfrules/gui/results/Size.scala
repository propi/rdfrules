package com.github.propi.rdfrules.gui.results

import com.github.propi.rdfrules.gui.ActionProgress
import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.Binding
import org.lrng.binding.html
import org.scalajs.dom.html.Div

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 14. 9. 2018.
  */
class Size(val title: String, val id: Future[String]) extends ActionProgress {

  @html
  def viewResult(result: Constants[js.Dynamic]): Binding[Div] = <div>
    {result.value.head.toString}
  </div>

}
