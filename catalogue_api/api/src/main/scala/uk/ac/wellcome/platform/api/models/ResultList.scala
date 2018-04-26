package uk.ac.wellcome.platform.api.models

import uk.ac.wellcome.work_model.IdentifiedWork

case class ResultList(
  results: List[IdentifiedWork],
  totalResults: Int
)
