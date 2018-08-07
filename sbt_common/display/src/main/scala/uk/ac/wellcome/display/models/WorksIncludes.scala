package uk.ac.wellcome.display.models

import com.fasterxml.jackson.core.JsonProcessingException

trait WorksIncludes

case class V1WorksIncludes(
  identifiers: Boolean = false,
  thumbnail: Boolean = false,
  items: Boolean = false
) extends WorksIncludes

case class V2WorksIncludes(
  identifiers: Boolean = false,
  items: Boolean = false
) extends WorksIncludes

class WorksIncludesParsingException(msg: String)
    extends JsonProcessingException(msg: String)

object V1WorksIncludes {
  val recognisedIncludes = List("identifiers", "thumbnail", "items")
  def apply(includesList: List[String]): V1WorksIncludes = V1WorksIncludes(
    identifiers = includesList.contains("identifiers"),
    thumbnail = includesList.contains("thumbnail"),
    items = includesList.contains("items")
  )

  def includeAll() = V1WorksIncludes(recognisedIncludes)
}

object V2WorksIncludes {
  val recognisedIncludes = List("identifiers", "items")
  def apply(includesList: List[String]): V2WorksIncludes = V2WorksIncludes(
    identifiers = includesList.contains("identifiers"),
    items = includesList.contains("items")
  )

  def includeAll() = V2WorksIncludes(recognisedIncludes)
}
