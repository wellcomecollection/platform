package uk.ac.wellcome.type_classes

import shapeless._
import shapeless.labelled.FieldType
import shapeless.ops.record.Updater
import shapeless.record._

// Type class with a method for updating the version on an instance of T.
//
// The type T must have a field "version".
//
// This isn't defined by the type hierarchy in the trait; instead it's checked
// at compile-time by shapeless and the companion object.
//
trait VersionUpdater[T] {
  def updateVersion(versioned: T, newVersion: Int): T
}

object VersionUpdater {
  val w = Witness(Symbol("version"))
  type version = w.T

  type versionField = FieldType[version, Int]

  def apply[A](implicit updater: VersionUpdater[A]): VersionUpdater[A] =
    updater

  def createVersionUpdater[T](f: (T, Int) => T): VersionUpdater[T] =
    new VersionUpdater[T] {
      def updateVersion(t: T, version: Int) = f(t, version)
    }

  // Generates a VersionUpdater for an arbitrary HList.
  //
  // Updater is a Shapeless type class that can update fields on an HList
  // (a "record" in Shapeless terminology).  It takes three type parameters:
  //
  //  - the original record (L)
  //  - the field to update (versionField)
  //  - the new record (L)
  //
  // In this case, because it returns the same type as it starts with,
  // we can only create a VersionUpdater for HLists that already have a
  // "version" field.  It can't add the field if it's not there already.
  //
  implicit def hlistVersionUpdater[L <: HList](
    implicit updater: Updater.Aux[L, versionField, L]) =
    createVersionUpdater[L] { (t: L, newVersion: Int) =>
      t.updated('version, newVersion)
    }

  // Generates an VersionUpdater for a case class ("product" in Shapeless).
  //
  // LabelledGeneric is a Shapeless type class that allows us to convert
  // between case classes and their HList representation.
  //
  // gen.to(c) converts the case class C to an HList L, and gen.from(...) goes
  // in the other direction.
  //
  implicit def productVersionUpdater[C, L <: HList](
    implicit gen: LabelledGeneric.Aux[C, L],
    versionUpdater: VersionUpdater[L]) = createVersionUpdater[C] {
    (c: C, newVersion: Int) =>
      gen.from(versionUpdater.updateVersion(gen.to(c), newVersion))
  }
}
