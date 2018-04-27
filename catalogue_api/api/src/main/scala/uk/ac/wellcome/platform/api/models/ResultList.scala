package uk.ac.wellcome.platform.api.models

import uk.ac.wellcome.models.work.internal.IdentifiedWork

case class ResultList(
  results: List[IdentifiedWork],
  totalResults: Int
)
