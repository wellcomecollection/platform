package uk.ac.wellcome.platform.api.responses

import com.fasterxml.jackson.annotation.{JsonProperty, JsonUnwrapped}
import uk.ac.wellcome.platform.api.models.DisplayWork

case class ResultResponse(
  @JsonProperty("@context") context: String,
  @JsonUnwrapped result: Any
)

case class ResultListResponse(
  @JsonProperty("@context") context: String,
  @JsonProperty("type") ontologyType: String = "ResultList",
  pageSize: Int = 10,
  totalPages: Int = 10,
  totalResults: Int = 100,
  results: Array[_ <: Any]
)

case class PaginatedWorksResult(
  pageSize: Int,
  totalPages: Int,
  totalResults: Int,
  results: Array[DisplayWork]
)
