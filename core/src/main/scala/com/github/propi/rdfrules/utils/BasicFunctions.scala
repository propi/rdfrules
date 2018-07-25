package com.github.propi.rdfrules.utils

import java.net.URLDecoder

import scala.util.Try

/**
  * Created by Vaclav Zeman on 19. 6. 2017.
  */
object BasicFunctions {

  object Match {
    def default: PartialFunction[Any, Unit] = {
      case _ =>
    }

    def apply[T](x: T)(body: PartialFunction[T, Unit]): Unit = (body orElse default) (x)
  }

  def fixUrlDecoding(x: String): String = {
    @scala.annotation.tailrec
    def checkNextChar(chars: List[Char], mem: String, result: String): String = {
      if (mem.length == 3) {
        if (Try(URLDecoder.decode(mem, "UTF-8")).isFailure) {
          checkNextChar(mem.charAt(1) :: mem.charAt(2) :: chars, "", result + "%25")
        } else {
          checkNextChar(chars, "", result + mem)
        }
      } else if (mem.length > 0 && mem.length < 3) {
        if (chars.isEmpty) {
          checkNextChar(mem.drop(1).toList ::: chars, "", result + "%25")
        } else {
          checkNextChar(chars.tail, mem + chars.head, result)
        }
      } else {
        if (chars.isEmpty) {
          result
        } else {
          chars.head match {
            case '%' => checkNextChar(chars.tail, "%", result)
            case x => checkNextChar(chars.tail, "", result + x)
          }
        }
      }
    }

    checkNextChar(x.toList, "", "")
  }

}
