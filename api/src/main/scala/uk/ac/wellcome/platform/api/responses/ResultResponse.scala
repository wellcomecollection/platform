package uk.ac.wellcome.platform.api.responses

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped

case class ResultResponse(
  @JsonProperty("@context") context: String,
  @JsonUnwrapped result: Any
)
