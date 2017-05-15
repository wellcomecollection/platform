package uk.ac.wellcome.platform.ingestor

import com.sksamuel.elastic4s.ElasticDsl.{deleteIndex, index}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import com.sksamuel.elastic4s.ElasticDsl._

class IngestorIndexTest extends FunSpec
  with IngestorUtils
  with Matchers
  with ScalaFutures {

  it("should create the index at startup in elasticsearch if it doesn't already exist") {
    elasticClient.execute(deleteIndex(indexName))

    eventually {
      val future = elasticClient.execute(index exists indexName)
      whenReady(future) { result =>
        result.isExists should be(false)
      }
    }

    val server = createServer
    server.start()

    eventually {
      val future = elasticClient.execute(index exists indexName)
      whenReady(future) { result =>
        result.isExists should be(true)
      }
    }
    server.close()
  }
}
