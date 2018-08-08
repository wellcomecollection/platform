package uk.ac.wellcome.display.shapeless
import shapeless.labelled.field
import shapeless.ops.hlist.{ConstMapper, Mapper, Zip}
import shapeless.ops.record.Keys
import shapeless.tag.Tagged
import shapeless.{HList, HNil, LabelledGeneric, Poly1, Witness, _}
import uk.ac.wellcome.display.models.WorksIncludes

trait WorksIncludesFromList[T] {
  def toWorksIncludes(includesList: List[String]): T
}

object WorksIncludesFromList {
  type Tag[T] = Symbol with Tagged[T]
  object isIncludedAttribute extends Poly1 {
    implicit def atTaggedSymbol[T](implicit witness: Witness.Aux[T]) = at[(Symbol with Tagged[T], List[String])]{case (t, includesList) =>
      val attributeName: String = t.name
      field[Tag[witness.T]](includesList.contains[String](attributeName))
    }
  }

  implicit def ghjg[W<: WorksIncludes, Repr <: HList, KeysRepr <: HList, HListL <: HList, ZippedHList <: HList](implicit gen: LabelledGeneric.Aux[W, Repr], keys: Keys.Aux[Repr, KeysRepr], constMapper: ConstMapper.Aux[List[String], KeysRepr, HListL], zipper: Zip.Aux[KeysRepr :: HListL :: HNil, ZippedHList], mapper: Mapper.Aux[isIncludedAttribute.type , ZippedHList, Repr]) = new WorksIncludesFromList[W] {
    override def toWorksIncludes(includesList: List[String]): W = {
      val includesHList = keys().mapConst(includesList)
      val attributesAndIncludesHList = keys().zip(includesHList)
      gen.from(attributesAndIncludesHList.map(isIncludedAttribute))
    }
  }

  def toWorksIncludes[W](includesList: List[String])(implicit c: WorksIncludesFromList[W]):W = c.toWorksIncludes(includesList)
}