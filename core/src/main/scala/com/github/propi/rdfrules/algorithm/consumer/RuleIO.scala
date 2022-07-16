package com.github.propi.rdfrules.algorithm.consumer

import com.github.propi.rdfrules.rule.Rule.FinalRule

trait RuleIO {
  def writer[T](f: RuleIO.Writer => T): T

  def reader[T](f: RuleIO.Reader => T): T
}

object RuleIO {

  trait Writer {
    def write(rule: FinalRule): Unit

    /**
      * Flush all buffers and sync all in memory on to disk
      */
    def flush(): Unit
  }

  trait Reader {
    def read(): Option[FinalRule]
  }

}
