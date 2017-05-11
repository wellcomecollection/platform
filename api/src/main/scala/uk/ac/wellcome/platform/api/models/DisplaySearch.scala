package uk.ac.wellcome.platform.api.models

import com.sksamuel.elastic4s.searches.RichSearchResponse


case class DisplaySearch(
  pageSize: Int,
  totalPages: Int,
  totalResults: Int,
  results: Array[DisplayWork]
)
case object DisplaySearch {
  def apply(searchResponse: RichSearchResponse, pageSize: Int): DisplaySearch = {
    DisplaySearch(
      results = searchResponse.hits.map { DisplayWork(_) },
      pageSize = pageSize,
      // TODO: This arithmetic is distinctly dodgy
      totalPages = (searchResponse.totalHits.toInt + pageSize) / pageSize,
      totalResults = searchResponse.totalHits.toInt
    )
  }
}
