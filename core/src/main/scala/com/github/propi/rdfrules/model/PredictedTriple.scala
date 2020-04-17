package com.github.propi.rdfrules.model

import com.github.propi.rdfrules.data.Triple
import com.github.propi.rdfrules.ruleset.ResolvedRule

/**
  * Created by Vaclav Zeman on 15. 10. 2019.
  */
case class PredictedTriple(triple: Triple)(val rule: ResolvedRule, val existing: Boolean)