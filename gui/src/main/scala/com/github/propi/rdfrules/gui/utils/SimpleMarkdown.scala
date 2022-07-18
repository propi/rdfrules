package com.github.propi.rdfrules.gui.utils

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.|

@js.native
@JSGlobal
object SimpleMarkdown extends js.Object {

  type UnknownContent = js.Array[Token] | String

  def defaultBlockParse(x: String): js.Any = js.native

  @js.native
  trait Token extends js.Object {
    val `type`: String = js.native
  }

  @js.native
  trait Heading extends Token {
    val level: Int = js.native
    val content: js.Array[Token] = js.native
  }

  @js.native
  trait Paragraph extends Token {
    val content: js.Array[Token] = js.native
  }

  @js.native
  trait Text extends Token {
    val content: String = js.native
  }

  @js.native
  trait List extends Token {
    val items: js.Array[js.Array[Token]] = js.native
    val ordered: Boolean = js.native
  }

  @js.native
  trait Strong extends Token {
    val content: js.Array[Token] = js.native
  }

  @js.native
  trait Em extends Token {
    val content: js.Array[Token] = js.native
  }

  @js.native
  trait Link extends Token {
    val content: js.Array[Token] = js.native
    val target: String = js.native
  }

  @js.native
  trait Unknown extends Token {
    val content: js.UndefOr[UnknownContent] = js.native
  }

  @js.native
  trait InlineCode extends Token {
    val content: String = js.native
  }

}