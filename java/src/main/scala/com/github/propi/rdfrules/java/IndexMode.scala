package com.github.propi.rdfrules.java

/**
  * Created by Vaclav Zeman on 11. 5. 2018.
  */
object IndexMode {

  def inUseInMemory: com.github.propi.rdfrules.index.Index.Mode = com.github.propi.rdfrules.index.Index.Mode.InUseInMemory

  def preservedInMemory: com.github.propi.rdfrules.index.Index.Mode = com.github.propi.rdfrules.index.Index.Mode.PreservedInMemory

}
