package uk.ac.wellcome.type_classes

import shapeless._
import shapeless.labelled.FieldType
import shapeless.ops.record.Selector
import shapeless.record._

// Type class that returns the version of an instances of T
trait VersionGetter[T] {
  def version(t: T): Int
}

object VersionGetter {
  val w = Witness(Symbol("version"))
  type version = w.T

  def apply[A](implicit enc: VersionGetter[A]): VersionGetter[A] =
    enc

  def createVersionGetter[T](f: T => Int): VersionGetter[T] = new VersionGetter[T] {
    def version(t: T) = f(t)
  }

  // Generates an VersionGetter for an HList of arbitrary length with the version field as the first field
  implicit def elementVersionGetter[L <: HList](implicit selector: Selector.Aux[FieldType[version, Int] :: L, version, Int]) = createVersionGetter {t: (FieldType[version, Int] :: L) =>
    t.get('version)
  }

  // Generates an VersionGetter for an HList returning the VersionGetter for the tail of the HList
  implicit def hlistVersionGetter[H, T <: HList](implicit versionGetter: VersionGetter[T]) = createVersionGetter[H::T]{t: (H::T)=>
    versionGetter.version(t.tail)
  }

  // Generates an VersionGetter for a case class using the VersionGetter for its HLists representation
  implicit def productVersionGetter[C, T](implicit labelledGeneric: LabelledGeneric.Aux[C,T], versionGetter: VersionGetter[T]) = createVersionGetter[C]{ t: C =>
    versionGetter.version(labelledGeneric.to(t))
  }
}