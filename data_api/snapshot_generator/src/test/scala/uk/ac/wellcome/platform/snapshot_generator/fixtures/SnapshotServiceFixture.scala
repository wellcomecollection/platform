package uk.ac.wellcome.platform.snapshot_generator.fixtures

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.http.ElasticClient
import org.scalatest.Suite
import uk.ac.wellcome.elasticsearch.DisplayElasticConfig
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.platform.snapshot_generator.services.SnapshotService
import uk.ac.wellcome.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait SnapshotServiceFixture extends ElasticsearchFixtures { this: Suite =>

  val mapper = new ObjectMapper with ScalaObjectMapper

  def withSnapshotService[R](s3AkkaClient: S3Client,
                             indexV1: Index,
                             indexV2: Index,
                             elasticClient: ElasticClient = elasticClient)(
    testWith: TestWith[SnapshotService, R])(
    implicit actorSystem: ActorSystem,
    materializer: ActorMaterializer): R = {
    val elasticConfig = DisplayElasticConfig(
      indexV1 = indexV1,
      indexV2 = indexV2
    )

    val snapshotService = new SnapshotService(
      elasticClient = elasticClient,
      elasticConfig = elasticConfig,
      akkaS3Client = s3AkkaClient
    )

    testWith(snapshotService)
  }
}
