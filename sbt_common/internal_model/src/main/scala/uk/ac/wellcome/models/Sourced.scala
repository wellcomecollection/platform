package uk.ac.wellcome.models

trait Sourced {
  val sourceId: String
  val sourceName: String
}

object Sourced {
  def id(sourceName: String, sourceId: String) = s"$sourceName/$sourceId"
}
