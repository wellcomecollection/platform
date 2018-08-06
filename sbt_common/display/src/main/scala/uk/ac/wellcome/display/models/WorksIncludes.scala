package uk.ac.wellcome.display.models

import com.fasterxml.jackson.core.JsonProcessingException
import shapeless.ops.hlist.Mapper
import shapeless.ops.hlist.ToTraversable
import shapeless.ops.record.Keys
import shapeless.tag.Tagged
import shapeless.{HList, LabelledGeneric, Poly1}

trait WorksIncludes

case class V1WorksIncludes(
  identifiers: Boolean = false,
  thumbnail: Boolean = false,
  items: Boolean = false
) extends WorksIncludes

case class V2WorksIncludes(
  identifiers: Boolean = false,
  thumbnail: Boolean = false,
  items: Boolean = false
) extends WorksIncludes

class WorksIncludesParsingException(msg: String)
    extends JsonProcessingException(msg: String)

object V1WorksIncludes{
  val recognisedIncludes = List("identifiers", "thumbnail", "items")
  def apply(includesList: List[String]):V1WorksIncludes = V1WorksIncludes(
    identifiers = includesList.contains("identifiers"),
    thumbnail = includesList.contains("thumbnail"),
    items = includesList.contains("items")
  )

  def includeAll() = V1WorksIncludes(recognisedIncludes)
}

object V2WorksIncludes{
  val recognisedIncludes = List("identifiers", "thumbnail", "items")
  def apply(includesList: List[String]):V2WorksIncludes = V2WorksIncludes(
    identifiers = includesList.contains("identifiers"),
    thumbnail = includesList.contains("thumbnail"),
    items = includesList.contains("items")
  )

  def includeAll() = V2WorksIncludes(recognisedIncludes)
}

trait ToAttributes[T] {
  def toAttributes(v: T): Seq[String]
}

object Attributes {
  object symbolName extends Poly1 {
    implicit def atTaggedSymbol[T] = at[Symbol with Tagged[T]](_.name)
  }

  implicit def familyFormat[T, Repr <: HList, KeysRepr <: HList, MapperRepr <: HList](
                                                                                       implicit gen: LabelledGeneric.Aux[T, Repr],
                                                                                       keys: Keys.Aux[Repr, KeysRepr],
                                                                                       mapper: Mapper.Aux[symbolName.type, KeysRepr, MapperRepr],
                                                                                       traversable: ToTraversable.Aux[MapperRepr, List, String]
                                                                                     ): ToAttributes[T] =
    (v: T) => {
      gen.to(v)
      keys().map(symbolName).toList
    }

  def toAttributes[T](v: T)(implicit c: ToAttributes[T]): Seq[String] = c.toAttributes(v)
}

//case object WorksIncludes {
//  def apply[W <: WorksIncludes](queryParam: String, recognisedIncludes: List[String], workIncludes: List[String] => W): W = {
//    val includesList = queryParam.split(",").toList
//    val unrecognisedIncludes = includesList
//      .filterNot(recognisedIncludes.contains)
//    if (unrecognisedIncludes.isEmpty) {
//      workIncludes(includesList)
//    } else {
//      val errorMessage = if (unrecognisedIncludes.length == 1) {
//        s"'${unrecognisedIncludes.head}' is not a valid include"
//      } else {
//        s"${unrecognisedIncludes.mkString("'", "', '", "'")} are not valid includes"
//      }
//      throw new WorksIncludesParsingException(errorMessage)
//    }
//  }
//}

//
//class WorksIncludesDeserializer extends JsonDeserializer[WorksIncludes] {
//  override def deserialize(p: JsonParser,
//                           ctxt: DeserializationContext): WorksIncludes =
//    WorksIncludes(p.getText())
//}
//
//class WorksIncludesDeserializerModule extends SimpleModule {
//  addDeserializer(classOf[WorksIncludes], new WorksIncludesDeserializer())
//}
