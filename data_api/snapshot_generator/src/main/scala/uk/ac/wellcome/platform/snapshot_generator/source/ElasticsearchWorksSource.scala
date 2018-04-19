package uk.ac.wellcome.platform.snapshot_generator.source

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.sksamuel.elastic4s.http.ElasticDsl.{search, termQuery}
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.search.SearchHit
import com.sksamuel.elastic4s.streams.ReactiveElastic._
import uk.ac.wellcome.models.IdentifiedWork
import uk.ac.wellcome.utils.JsonUtil._

object ElasticsearchWorksSource {
  def apply(elasticClient: HttpClient, indexName: String, itemType: String)(implicit actorSystem: ActorSystem): Source[IdentifiedWork, NotUsed] = {
    Source.fromPublisher(
    elasticClient.publisher(search(s"$indexName/$itemType").query(termQuery("visible", true)).scroll("10m"))).map{searchHit: SearchHit => fromJson[IdentifiedWork](searchHit.sourceAsString).get}
  }
}
