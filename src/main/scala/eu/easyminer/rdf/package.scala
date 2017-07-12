package eu.easyminer

import akka.actor.ActorSystem

/**
  * Created by Vaclav Zeman on 11. 7. 2017.
  */
package object rdf {

  implicit lazy val actorSystem = ActorSystem("easyminer-actor-system")

}