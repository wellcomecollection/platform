package uk.ac.wellcome.type_classes

import shapeless._
import shapeless.labelled.FieldType
import shapeless.ops.record.Selector
import shapeless.record._

// Type class with a method for getting the version of an instance of T.
//
// The type T must have a field "version".
//
// This isn't defined by the type hierarchy in the trait; instead it's checked
// at compile-time by shapeless and the companion object.
//
trait VersionGetter[T] {
  def version(t: T): Int
}

object VersionGetter {
  val w = Witness(Symbol("version"))
  type version = w.T

  def apply[A](implicit enc: VersionGetter[A]): VersionGetter[A] =
    enc

  def createVersionGetter[T](f: T => Int): VersionGetter[T] =
    new VersionGetter[T] {
      def version(t: T) = f(t)
    }

  // Generates an VersionGetter for an HList.
  //
  // Selector is a Shapeless type class that selects an individual field
  // from an HList.  It takes three type parameters:
  //
  //  - the type of the input (L)
  //  - the field to select (version)
  //  - the type of the returned value (Int)
  //
  implicit def hlistVersionGetter[L <: HList](
    implicit selector: Selector.Aux[L, version, Int]) = createVersionGetter {
    t: L =>
      selector(t)
  }

  // Generates an VersionGetter for a case class.
  //
  // LabelledGeneric is a Shapeless type class that allows us to convert
  // between case classes and their HList representation.
  //
  // labelledGeneric.to(c) converts the case class C to an HList L, and then we
  // can use the constructor above.
  //
  implicit def productVersionGetter[C, L](
    implicit labelledGeneric: LabelledGeneric.Aux[C, L],
    versionGetter: VersionGetter[L]) = createVersionGetter[C] { c: C =>
    versionGetter.version(labelledGeneric.to(c))
  }
}
