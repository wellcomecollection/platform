package uk.ac.wellcome.platform.ingestor.services

import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.models.UnifiedItem
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future

@Singleton
class MessageProcessorService @Inject()(
  @Flag("es.index") esIndex: String,
  @Flag("es.type") esType: String,
  elasticClient: ElasticClient
) {
  def indexUnifiedItem(document: String): Future[Unit] = {
    implicit val jsonMapper = UnifiedItem

    Future.fromTry(JsonUtil.fromJson[UnifiedItem](document))
      .map(item => {
        elasticClient.execute {
          indexInto(esIndex / esType).doc(item)
        }
      })
  }
}
