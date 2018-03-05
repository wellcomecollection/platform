package uk.ac.wellcome.platform.api.models

import uk.ac.wellcome.models.IdentifiedWork

case class ResultList(
  results: List[IdentifiedWork],
  totalResults: Int
)
