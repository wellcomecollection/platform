package uk.ac.wellcome.models.transformable.miro

import java.io.InputStream

import scala.io.Source
import scala.util.Try
import uk.ac.wellcome.models.{IdentifierSchemes, _}
import uk.ac.wellcome.models.transformable._
import uk.ac.wellcome.utils.JsonUtil

case class MiroTransformable(MiroID: String,
                             MiroCollection: String,
                             data: String,
                             ReindexShard: String = "default",
                             ReindexVersion: Int = 0)
    extends Transformable
    with Reindexable[String] {

  val id: ItemIdentifier[String] = ItemIdentifier(
    HashKey("MiroID", MiroID),
    RangeKey("MiroCollection", MiroCollection)
  )
}
