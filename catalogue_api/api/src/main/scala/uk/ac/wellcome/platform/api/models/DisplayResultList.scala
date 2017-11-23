package uk.ac.wellcome.platform.api.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.sksamuel.elastic4s.http.search.SearchResponse
import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(
  value = "ResultList",
  description = "A list of things."
)
case class DisplayResultList(
  @ApiModelProperty(value = "Number of things returned per page") pageSize: Int,
  @ApiModelProperty(value = "Total number of pages in the result list") totalPages: Int,
  @ApiModelProperty(value = "Total number of results in the result list") totalResults: Int,
  @ApiModelProperty(value = "List of things in the current page") results: Array[
    DisplayWork]) {
  @ApiModelProperty(name = "type", value = "A type of thing", readOnly = true)
  val ontologyType: String = "ResultList"
}

case object DisplayResultList {
  def apply(searchResponse: SearchResponse,
            pageSize: Int,
            includes: WorksIncludes): DisplayResultList = {
    DisplayResultList(
      results = searchResponse.hits.hits.map { DisplayWork(_, includes) },
      pageSize = pageSize,
      totalPages =
        Math.ceil(searchResponse.totalHits.toDouble / pageSize.toDouble).toInt,
      totalResults = searchResponse.totalHits.toInt
    )
  }
}
