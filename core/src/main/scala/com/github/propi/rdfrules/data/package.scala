package com.github.propi.rdfrules

import com.github.propi.rdfrules.data.formats.{Cache, Compressed, JenaLang, Sql, Tsv}

/**
  * Created by Vaclav Zeman on 4. 8. 2018.
  */
package object data extends JenaLang with Tsv with Sql with Cache with Compressed {
}