package uk.ac.wellcome.transformer.transformers.sierra

import com.twitter.inject.Logging
import scala.util.{Failure, Success}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.transformer.source.SierraItemData
import uk.ac.wellcome.utils.JsonUtil._

trait SierraItems extends Logging {
  def extractItemData(sierraTransformable: SierraTransformable): List[SierraItemData] = {
    sierraTransformable
      .itemData
      .values
      .map { _.data }
      .map { jsonString =>
        fromJson[SierraItemData](jsonString) match {
          case Success(d) => Some(d)
          case Failure(e) => {
            error(s"Failed to parse item!", e)
            None
          }
        }
      }
      .toList
      .flatten
  }
}
