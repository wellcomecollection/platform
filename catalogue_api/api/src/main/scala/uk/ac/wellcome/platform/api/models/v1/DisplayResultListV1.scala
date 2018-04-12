package uk.ac.wellcome.platform.api.models.v1

import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.display.models.WorksIncludes
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.platform.api.models.ResultList

@ApiModel(
  value = "ResultList",
  description = "A list of things."
)
case class DisplayResultListV1(
  @ApiModelProperty(value = "Number of things returned per page") pageSize: Int,
  @ApiModelProperty(value = "Total number of pages in the result list") totalPages: Int,
  @ApiModelProperty(value = "Total number of results in the result list") totalResults: Int,
  @ApiModelProperty(value = "List of things in the current page") results: Array[
    DisplayWorkV1]) {
  @ApiModelProperty(name = "type", value = "A type of thing", readOnly = true)
  val ontologyType: String = "ResultList"
}

case object DisplayResultListV1 {
  def apply(resultList: ResultList,
            pageSize: Int,
            includes: WorksIncludes): DisplayResultListV1 =
    DisplayResultListV1(
      results = resultList.results.map { DisplayWorkV1(_, includes) }.toArray,
      pageSize = pageSize,
      totalPages = Math
        .ceil(resultList.totalResults.toDouble / pageSize.toDouble)
        .toInt,
      totalResults = resultList.totalResults
    )
}
