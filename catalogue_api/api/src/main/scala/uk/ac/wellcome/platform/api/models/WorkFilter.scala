package uk.ac.wellcome.platform.api.models

import uk.ac.wellcome.models.work.internal.WorkType

sealed trait WorkFilter

case class WorkTypeFilter(workTypes: Seq[WorkType]) extends WorkFilter

case object WorkTypeFilter {
  def apply(arg: String): WorkTypeFilter =
    WorkTypeFilter(
      workTypes = arg.split(",").map { id => WorkType(id, label = "") }
    )
}
