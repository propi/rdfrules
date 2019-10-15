package com.github.propi.rdfrules.model

import com.github.propi.rdfrules.data.Triple
import com.github.propi.rdfrules.rule.Measure
import com.github.propi.rdfrules.utils.TypedKeyMap

/**
  * Created by Vaclav Zeman on 15. 10. 2019.
  */
case class PredictedTriple(triple: Triple)(val measures: TypedKeyMap.Immutable[Measure])