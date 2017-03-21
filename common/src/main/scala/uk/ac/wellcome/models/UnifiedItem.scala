package uk.ac.wellcome.models

import com.sksamuel.elastic4s._
import uk.ac.wellcome.utils.JsonUtil

case class UnifiedItem(
  source: String,
  accessStatus: Option[String]
)
object UnifiedItem extends Indexable[UnifiedItem] {
  // TODO: Resolve this try sensibly
  override def json(t: UnifiedItem): String =
    JsonUtil.toJson(t).get
}
