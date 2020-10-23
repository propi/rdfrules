package com.github.propi.rdfrules.algorithm.consumer

import com.github.propi.rdfrules.rule.Rule

trait PrettyPrintedWriter {
  def write(rule: Rule.Simple): Unit

  /**
    * Flush all buffers and sync all in memory on to disk
    */
  def flush(): Unit

  /**
    * Close the output stream
    */
  def close(): Unit
}
