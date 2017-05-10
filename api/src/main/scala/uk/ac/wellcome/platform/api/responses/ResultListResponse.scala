package uk.ac.wellcome.platform.api.responses

import com.fasterxml.jackson.annotation.JsonProperty

import scala.language.existentials

case class ResultListResponse(
  @JsonProperty("@context") context: String,
  @JsonProperty("type") ontologyType: String = "ResultList",
  pageSize: Int = 10,
  totalPages: Int = 10,
  totalResults: Int = 100,
  results: Array[_ <: Any]
)