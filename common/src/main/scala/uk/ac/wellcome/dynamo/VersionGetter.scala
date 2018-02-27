package uk.ac.wellcome.dynamo

import shapeless.labelled.FieldType
import shapeless._
import shapeless.record._

trait VersionGetter[T] {
  def version(t: T): Int
}

object VersionGetter {
  val w = Witness(Symbol("version"))
  type id = w.T

  def createVersionGetter[T](f: T => Int): VersionGetter[T] = new VersionGetter[T] {
    def version(t: T) = f(t)
  }

  implicit def lastElementVersionGetter[L <: FieldType[id, Int] :: HNil] = createVersionGetter[L] {t: L =>
    t.get('version)
  }

  implicit def notLastElementVersionGetter[L <: FieldType[id, Int] :: T :: HNil, T <: HList] = createVersionGetter[L] {t: L =>
    t.get('version)
  }

  implicit def hlistVersionGetter[L <: H :: T, H, T <: HList](implicit versionGetter: VersionGetter[T]) = createVersionGetter[L]{ t: L =>
    versionGetter.version(t.tail)
  }

  implicit def productVersionGetter[C, T](implicit labelledGeneric: LabelledGeneric.Aux[C,T], versionGetter: VersionGetter[T]) = createVersionGetter[C]{ t: C =>
    versionGetter.version(labelledGeneric.to(t))
  }
}