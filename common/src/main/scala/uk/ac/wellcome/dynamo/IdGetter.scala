package uk.ac.wellcome.dynamo

import shapeless._
import shapeless.labelled.FieldType
import shapeless.record._


trait IdGetter[T] {
  def id(t: T): String
}

object IdGetter {
  val w = Witness(Symbol("id"))
  type id = w.T

  def createIdGetter[T](f: T => String): IdGetter[T] = new IdGetter[T] {
    def id(t: T) = f(t)
  }

  implicit def lastElementIdGetter[L <: FieldType[id, String] :: HNil] = createIdGetter[L] {t: L =>
    t.get('id)
  }

  implicit def notLastElementIdGetter[L <: FieldType[id, String] :: T :: HNil, T <: HList] = createIdGetter[L] {t: L =>
    t.get('id)
  }

  implicit def hlistIdGetter[L <: H :: T, H, T <: HList](implicit idGetter: IdGetter[T]) = createIdGetter[L]{t: L =>
    idGetter.id(t.tail)
  }

  implicit def productIdGetter[C, T](implicit labelledGeneric: LabelledGeneric.Aux[C,T],idGetter: IdGetter[T]) = createIdGetter[C]{t: C =>
    idGetter.id(labelledGeneric.to(t))
  }
}