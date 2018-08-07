package com.github.propi.rdfrules.java

import com.github.propi.rdfrules.data.{DiscretizationTask => ScalaDiscretizationTask}


/**
  * Created by Vaclav Zeman on 11. 5. 2018.
  */
object DiscretizationTaskConverters {

  def inMemory: ScalaDiscretizationTask.Mode = ScalaDiscretizationTask.Mode.InMemory

  def external: ScalaDiscretizationTask.Mode = ScalaDiscretizationTask.Mode.External

  def equifrequency(bins: Int, buffer: Int): ScalaDiscretizationTask.Equifrequency = ScalaDiscretizationTask.Equifrequency(bins, buffer)

  def equifrequency(bins: Int): ScalaDiscretizationTask.Equifrequency = ScalaDiscretizationTask.Equifrequency(bins)

  def equisize(support: Double, buffer: Int): ScalaDiscretizationTask.Equisize = ScalaDiscretizationTask.Equisize(support, buffer)

  def equisize(support: Double): ScalaDiscretizationTask.Equisize = ScalaDiscretizationTask.Equisize(support)

}
