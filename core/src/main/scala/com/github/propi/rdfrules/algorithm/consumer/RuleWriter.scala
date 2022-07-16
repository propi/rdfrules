package com.github.propi.rdfrules.algorithm.consumer

import com.github.propi.rdfrules.rule.Rule.FinalRule

trait RuleWriter {
  def write(rule: FinalRule): Unit

  /**
    * Flush all buffers and sync all in memory on to disk
    */
  def flush(): Unit

  /**
    * Close the output stream
    */
  def close(): Unit
}
