package uk.ac.wellcome.type_classes

import shapeless._
import shapeless.labelled.FieldType
import shapeless.ops.record.Updater
import shapeless.record._

// Type class that updates the version field on an instance of T
trait VersionUpdater[T] {
  def updateVersion(versioned: T, newVersion: Int): T
}

object VersionUpdater {
  val w = Witness(Symbol("version"))
  type version = w.T

  type versionField = FieldType[version, Int]

  def apply[A](implicit enc: VersionUpdater[A]): VersionUpdater[A] =
    enc

  def createVersionUpdater[T](f: (T,Int) => T): VersionUpdater[T] = new VersionUpdater[T] {
      def updateVersion(t: T, version: Int) = f(t,version)
  }

  // Generates a VersionUpdater for an arbitrary HList
  implicit def hlistVersionUpdater[L <: HList](implicit updater: Updater.Aux[L, versionField, L]) = createVersionUpdater[L] {(t: L, newVersion: Int) =>
    t.updated('version, newVersion)
  }

  // Generates an VersionUpdater for a case class using the VersionUpdater for its HLists representation
  implicit def productVersionUpdater[C, T](implicit gen: LabelledGeneric.Aux[C,T], versionGetter: VersionUpdater[T]) = createVersionUpdater[C]{(t: C, newVersion: Int)=>
    gen.from(versionGetter.updateVersion(gen.to(t), newVersion))
  }
}