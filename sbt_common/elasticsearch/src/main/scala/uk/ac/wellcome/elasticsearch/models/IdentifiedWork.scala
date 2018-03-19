package uk.ac.wellcome.elasticsearch.models

import com.sksamuel.elastic4s.Indexable
import uk.ac.wellcome.models.IdentifiedWork

case object IdentifiedWork extends Indexable[IdentifiedWork] {
  override def json(t: IdentifiedWork): String =
    toJson(t).get
}
