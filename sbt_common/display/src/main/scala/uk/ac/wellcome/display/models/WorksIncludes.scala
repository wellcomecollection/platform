package uk.ac.wellcome.display.models

import com.fasterxml.jackson.core.JsonProcessingException
import uk.ac.wellcome.display.shapeless.{ToAttributes, WorksIncludesFromList}

trait WorksIncludes

case class V1WorksIncludes(
  identifiers: Boolean = false,
  thumbnail: Boolean = false,
  items: Boolean = false
) extends WorksIncludes

case class V2WorksIncludes(
  identifiers: Boolean = false,
  items: Boolean = false,
  subjects: Boolean = false,
  genres: Boolean = false,
  contributors: Boolean = false
) extends WorksIncludes

class WorksIncludesParsingException(msg: String)
    extends JsonProcessingException(msg: String)

object V1WorksIncludes {
  val recognisedIncludes = ToAttributes.toAttributes[V1WorksIncludes]
  def apply(includesList: List[String]): V1WorksIncludes = WorksIncludesFromList.toWorksIncludes[V1WorksIncludes](includesList)

  def includeAll() = V1WorksIncludes(recognisedIncludes)
}

object V2WorksIncludes {
  val recognisedIncludes = ToAttributes.toAttributes[V2WorksIncludes]
  def apply(includesList: List[String]): V2WorksIncludes = WorksIncludesFromList.toWorksIncludes[V2WorksIncludes](includesList)

  def includeAll() = V2WorksIncludes(recognisedIncludes)
}
