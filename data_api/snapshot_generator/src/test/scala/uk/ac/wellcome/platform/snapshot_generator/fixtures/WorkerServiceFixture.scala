package uk.ac.wellcome.platform.snapshot_generator.fixtures

import akka.actor.ActorSystem
import akka.stream.alpakka.s3.scaladsl.S3Client
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.scalatest.Suite
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.platform.snapshot_generator.services.SnapshotService
import uk.ac.wellcome.test.fixtures.TestWith

trait WorkerServiceFixture extends ElasticsearchFixtures { this: Suite =>
  val mapper = new ObjectMapper with ScalaObjectMapper

  def withSnapshotService[R](
                              s3AkkaClient: S3Client,
                              indexNameV1: String,
                              indexNameV2: String)(testWith: TestWith[SnapshotService, R])(
                              implicit actorSystem: ActorSystem): R = {
    val elasticConfig = createDisplayElasticConfigWith(
      indexV1name = indexNameV1,
      indexV2name = indexNameV2
    )

    val snapshotService = new SnapshotService(
      elasticClient = elasticClient,
      elasticConfig = elasticConfig,
      akkaS3Client = s3AkkaClient,
      objectMapper = mapper
    )

    testWith(snapshotService)
  }
}
