package uk.ac.wellcome.models.transformable.sierra

import uk.ac.wellcome.utils.JsonUtil

import scala.util.{Failure, Success, Try}

case class SierraItemTransformableData(
  deleted: Option[Boolean]
)

case object SierraItemTransformableData {
  def create(data: String): SierraItemTransformableData =
    JsonUtil.fromJson[SierraItemTransformableData](data) match {
      case Success(sierraData) => sierraData
      case Failure(e) => throw e
    }
}
