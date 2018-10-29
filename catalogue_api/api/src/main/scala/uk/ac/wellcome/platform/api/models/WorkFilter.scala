package uk.ac.wellcome.platform.api.models

sealed trait WorkFilter

case class WorkTypeFilter(workTypeIds: Seq[String]) extends WorkFilter

case object WorkTypeFilter {
  def apply(workTypeId: String): WorkTypeFilter =
    WorkTypeFilter(workTypeIds = List(workTypeId))
}
