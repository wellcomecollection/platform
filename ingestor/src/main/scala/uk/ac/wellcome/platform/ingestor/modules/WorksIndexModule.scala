package uk.ac.wellcome.platform.ingestor.modules

import com.sksamuel.elastic4s.TcpClient
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.elasticsearch.mappings.WorksIndex

object WorksIndexModule extends TwitterModule {

  override def singletonStartup(injector: Injector) {
    info("Creating/Updating Elasticsearch index")

    val indexName = flag[String]("es.index", "records", "ES index name")
    val itemType = flag[String]("es.type", "item", "ES document type")
    val elasticClient = injector.instance[TcpClient]

    new WorksIndex(elasticClient, indexName(), itemType()).create
  }
}
