package com.github.propi.rdfrules.java

import java.util
import java.util.function.Function

import scala.collection.JavaConverters._

/**
  * Created by Vaclav Zeman on 3. 5. 2018.
  */
class MapWrapper[K, V](_map: collection.Map[K, V]) {

  private class MappedKeyMap[K1, V1](_map: collection.Map[K, V1], f1: Function[K, K1], f2: Function[K1, K]) extends collection.Map[K1, V1] {
    def get(key: K1): Option[V1] = _map.get(f2(key))

    def iterator: Iterator[(K1, V1)] = _map.iterator.map(x => f1(x._1) -> x._2)

    def +[V2 >: V1](kv: (K1, V2)): collection.Map[K1, V2] = new MappedKeyMap(_map + (f2(kv._1) -> kv._2), f1, f2)

    def -(key: K1): collection.Map[K1, V1] = new MappedKeyMap(_map - f2(key), f1, f2)
  }

  def mapValues[V1](f: Function[V, V1]): MapWrapper[K, V1] = new MapWrapper(_map.mapValues(f.apply))

  def mapKeys[K1](f1: Function[K, K1], f2: Function[K1, K]): MapWrapper[K1, V] = new MapWrapper(new MappedKeyMap(_map, f1, f2))

  def asJava: util.Map[K, V] = _map.asJava

}