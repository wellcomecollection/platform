package uk.ac.wellcome.models.transformable.sierra

import uk.ac.wellcome.utils.JsonUtil

import scala.util.{Failure, Success, Try}

case class SierraBibTransformableData(
  title: Option[String]
)

case object SierraBibTransformableData {
  def create(data: String): SierraBibTransformableData =
    JsonUtil.fromJson[SierraBibTransformableData](data) match {
      case Success(sierraData) => sierraData
      case Failure(e) => throw e
    }
}
