package uk.ac.wellcome.platform.snapshot_generator.source

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticDsl.{search, termQuery}
import com.sksamuel.elastic4s.http.search.SearchHit
import com.sksamuel.elastic4s.streams.ReactiveElastic._
import com.twitter.inject.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal.IdentifiedWork

object ElasticsearchWorksSource extends Logging {
  def apply(elasticClient: ElasticClient, index: Index)(
    implicit actorSystem: ActorSystem): Source[IdentifiedWork, NotUsed] = {
    val loggingSink = Flow[IdentifiedWork]
      .grouped(10000)
      .map(works => {
        logger.info(s"Received ${works.length} works from $index")
        works
      })
      .to(Sink.ignore)
    Source
      .fromPublisher(
        elasticClient.publisher(
          search(index)
            .query(termQuery("type", "IdentifiedWork"))
            .scroll(keepAlive = "5m")
            // Increasing the size of each request from the
            // default 100 to 1000 as it makes it go significantly faster
            .size(1000))
      )
      .map { searchHit: SearchHit =>
        fromJson[IdentifiedWork](searchHit.sourceAsString).get
      }
      .alsoTo(loggingSink)
  }
}
