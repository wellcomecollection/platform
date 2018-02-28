package uk.ac.wellcome.dynamo

import shapeless.labelled.FieldType
import shapeless._
import shapeless.ops.record.Selector
import shapeless.record._

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

  implicit def elementVersionGetter[L <: HList](implicit selector: Selector.Aux[FieldType[version, Int] :: L, version, Int]) = createVersionGetter {t: (FieldType[version, Int] :: L) =>
    t.get('version)
  }

  implicit def hlistVersionGetter[H, T <: HList](implicit versionGetter: VersionGetter[T]) = createVersionGetter[H::T]{t: (H::T)=>
    versionGetter.version(t.tail)
  }

  implicit def productVersionGetter[C, T](implicit labelledGeneric: LabelledGeneric.Aux[C,T], versionGetter: VersionGetter[T]) = createVersionGetter[C]{ t: C =>
    versionGetter.version(labelledGeneric.to(t))
  }
}