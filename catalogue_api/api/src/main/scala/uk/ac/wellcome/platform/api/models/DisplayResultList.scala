package uk.ac.wellcome.platform.api.models

import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.display.models.{DisplayWork, WorksIncludes}
import uk.ac.wellcome.models.work.internal.IdentifiedWork

@ApiModel(
  value = "ResultList",
  description = "A list of things."
)
case class DisplayResultList[T <: DisplayWork](
  @ApiModelProperty(value = "Number of things returned per page") pageSize: Int,
  @ApiModelProperty(value = "Total number of pages in the result list") totalPages: Int,
  @ApiModelProperty(value = "Total number of results in the result list") totalResults: Int,
  @ApiModelProperty(value = "List of things in the current page") results: List[
    T]) {
  @ApiModelProperty(name = "type", value = "A type of thing", readOnly = true)
  val ontologyType: String = "ResultList"
}

case object DisplayResultList {
  def apply[T <: DisplayWork](
    resultList: ResultList,
    toDisplayWork: (IdentifiedWork, WorksIncludes) => T,
    pageSize: Int,
    includes: WorksIncludes): DisplayResultList[T] =
    DisplayResultList(
      results = resultList.results.map { toDisplayWork(_, includes) },
      pageSize = pageSize,
      totalPages = Math
        .ceil(resultList.totalResults.toDouble / pageSize.toDouble)
        .toInt,
      totalResults = resultList.totalResults
    )
}
