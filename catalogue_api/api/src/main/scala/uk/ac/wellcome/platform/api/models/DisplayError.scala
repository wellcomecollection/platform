package uk.ac.wellcome.platform.api.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}

import uk.ac.wellcome.models.Error

@ApiModel(
  value = "Error"
)
case class DisplayError(
  @ApiModelProperty(
    value = "The type of error",
    allowableValues = "http"
  ) errorType: String,
  @ApiModelProperty(
    dataType = "Int",
    value = "The HTTP response status code"
  ) httpStatus: Option[Int] = None,
  @ApiModelProperty(
    value = "The title or other short name of the error"
  ) label: String,
  @ApiModelProperty(
    dataType = "String",
    value = "The specific error"
  ) description: Option[String] = None
) {
  @JsonProperty("type") val ontologyType: String = "Error"
}

case object DisplayError {
  def apply(error: Error): DisplayError = DisplayError(
    errorType = error.errorType,
    httpStatus = error.httpStatus,
    label = error.label,
    description = error.description
  )
}
