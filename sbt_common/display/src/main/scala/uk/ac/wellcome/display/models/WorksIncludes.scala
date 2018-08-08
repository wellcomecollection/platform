package uk.ac.wellcome.display.models

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
  contributors: Boolean = false,
  production: Boolean = false
) extends WorksIncludes

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
  val recognisedIncludes = List(
    "identifiers",
    "items",
    "subjects",
    "genres",
    "contributors",
    "production")
  def apply(includesList: List[String]): V2WorksIncludes = V2WorksIncludes(
    identifiers = includesList.contains("identifiers"),
    items = includesList.contains("items"),
    subjects = includesList.contains("subjects"),
    genres = includesList.contains("genres"),
    contributors = includesList.contains("contributors"),
    production = includesList.contains("production")
  )

  def includeAll() = V2WorksIncludes(recognisedIncludes)
}
