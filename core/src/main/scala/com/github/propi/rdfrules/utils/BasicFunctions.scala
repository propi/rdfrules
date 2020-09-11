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

  def round(x: Double, precision: Int): Double = BigDecimal(x).setScale(precision, BigDecimal.RoundingMode.HALF_UP).toDouble

  def parseNumber(x: String): Option[Either[Int, Double]] = {
    if (x == null || x.isEmpty) {
      return None
    }
    var hasExp = false
    var hasDecPoint = false
    var allowSigns = false
    var foundDigit = false
    val sz = x.length - 1

    def result = {
      if (hasDecPoint) {
        Try(Right(x.toDouble)).toOption
      } else {
        Try(Left(x.toInt)).orElse(Try(Right(x.toDouble))).toOption
      }
    }

    // deal with any possible sign up front
    val start = if (x.head == '-') 1 else 0
    // for type qualifiers
    var i = start
    // loop to the next to last char or to the last char if we need another digit to
    // make a valid number (e.g. chars[0..5] = "1234E")
    while (i < sz || (i < sz + 1 && allowSigns && !foundDigit)) {
      val ch = x.charAt(i)
      if (ch >= '0' && ch <= '9') {
        foundDigit = true
        allowSigns = false
      } else if (ch == '.') {
        if (hasDecPoint || hasExp) { // two decimal points or dec in exponent
          return None
        }
        hasDecPoint = true
      } else if (ch == 'e' || ch == 'E') { // we've already taken care of hex.
        if (hasExp) { // two E's
          return None
        }
        if (!foundDigit) {
          return None
        }
        hasExp = true
        allowSigns = true
      } else if (ch == '+' || ch == '-') {
        if (!allowSigns) return None
        allowSigns = false
        foundDigit = false // we need a digit after the E
      } else {
        return None
      }
      i += 1
    }
    if (i < x.length) {
      val ch = x.charAt(i)
      if (ch >= '0' && ch <= '9') { // no type qualifier, OK
        return result
      }
      if (ch == 'e' || ch == 'E') { // can't have an E at the last byte
        return None
      }
      if (ch == '.') {
        if (hasDecPoint || hasExp) { // two decimal points or dec in exponent
          return None
        }
        // single trailing decimal point after non-exponent is ok
        return if (foundDigit) result else None
      }
      if (!allowSigns && (ch == 'd' || ch == 'D' || ch == 'f' || ch == 'F')) return if (foundDigit) result else None
      // last character is illegal
      return None
    }
    // allowSigns is true iff the val ends in 'E'
    // found digit it to make sure weird stuff like '.' and '1E-' doesn't pass
    if (!allowSigns && foundDigit) result else None
  }

}
