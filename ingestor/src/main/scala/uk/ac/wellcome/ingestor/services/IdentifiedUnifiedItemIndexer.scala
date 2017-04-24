package uk.ac.wellcome.platform.ingestor.services

import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.indexes.RichIndexResponse
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.models.{IdentifiedUnifiedItem, UnifiedItem}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future

@Singleton
class IdentifiedUnifiedItemIndexer @Inject()(
  @Flag("es.index") esIndex: String,
  @Flag("es.type") esType: String,
  elasticClient: ElasticClient
) extends Logging {

  def indexIdentifiedUnifiedItem(document: String): Future[RichIndexResponse] = {
    implicit val jsonMapper = IdentifiedUnifiedItem

    Future
      .fromTry(JsonUtil.fromJson[IdentifiedUnifiedItem](document))
      .flatMap(item => {
        info(s"Indexing item $item")
        elasticClient.execute {
          indexInto(esIndex / esType).doc(item)
        }
      })
      .recover {
        case e: Throwable =>
          error(s"error indexing document $document into elasticsearch", e)
          throw e
      }
  }
}
