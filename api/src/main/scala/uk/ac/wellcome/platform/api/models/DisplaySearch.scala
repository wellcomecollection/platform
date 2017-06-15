package uk.ac.wellcome.platform.api.models

import com.sksamuel.elastic4s.http.search.SearchResponse


case class DisplaySearch(
  pageSize: Int,
  totalPages: Int,
  totalResults: Int,
  results: Array[DisplayWork]
)
case object DisplaySearch {
  def apply(searchResponse: SearchResponse, pageSize: Int, includes: List[String]): DisplaySearch = {
    DisplaySearch(
      results = searchResponse.hits.hits.map { DisplayWork(_, includes) },
      pageSize = pageSize,
      totalPages = Math.ceil(searchResponse.totalHits.toDouble / pageSize.toDouble).toInt,
      totalResults = searchResponse.totalHits.toInt
    )
  }
}
