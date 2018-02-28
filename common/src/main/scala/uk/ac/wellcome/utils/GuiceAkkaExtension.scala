package uk.ac.wellcome.utils

import akka.actor._
import com.google.inject.name.Names
import com.google.inject.{Injector, Key}

/**
  * An Akka extension implementation for Guice-based injection. The Extension provides Akka access to
  * dependencies defined in Guice.
  */
class GuiceAkkaExtensionImpl extends Extension {

  private var injector: Injector = _

  def initialize(injector: Injector) {
    this.injector = injector
  }

  def props(actorName: String) =
    Props(classOf[GuiceActorProducer], injector, actorName)

}

object GuiceAkkaExtension
    extends ExtensionId[GuiceAkkaExtensionImpl]
    with ExtensionIdProvider {

  /** Register ourselves with the ExtensionIdProvider */
  override def lookup() = GuiceAkkaExtension

  /** Called by Akka in order to create an instance of the extension. */
  override def createExtension(system: ExtendedActorSystem) =
    new GuiceAkkaExtensionImpl

  /** Java API: Retrieve the extension for the given system. */
  override def get(system: ActorSystem): GuiceAkkaExtensionImpl =
    super.get(system)

}

/**
  * A convenience trait for an actor companion object to extend to provide names.
  */
trait NamedActor {
  def name: String
}

/**
  * Mix in with Guice Modules that contain providers for top-level actor refs.
  */
trait GuiceAkkaActorRefProvider {
  def propsFor(system: ActorSystem, name: String) =
    GuiceAkkaExtension(system).props(name)
  def provideActorRef(system: ActorSystem, name: String): ActorRef =
    system.actorOf(propsFor(system, name))
}

/**
  * A creator for actors that allows us to return actor prototypes that are created by Guice
  * (and therefore injected with any dependencies needed by that actor). Since all untyped actors
  * implement the Actor trait, we need to use a name annotation on each actor (defined in the Guice
  * module) so that the name-based lookup obtains the correct actor from Guice.
  */
class GuiceActorProducer(val injector: Injector, val actorName: String)
    extends IndirectActorProducer {

  override def actorClass = classOf[Actor]

  override def produce() =
    injector
      .getBinding(Key.get(classOf[Actor], Names.named(actorName)))
      .getProvider
      .get()

}
