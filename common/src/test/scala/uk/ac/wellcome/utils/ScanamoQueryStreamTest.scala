package uk.ac.wellcome.utils

import com.gu.scanamo.Scanamo
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.query._
import com.gu.scanamo.request.{ScanamoQueryOptions, ScanamoQueryRequest}
import com.gu.scanamo.syntax._
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.models.MiroTransformable
import uk.ac.wellcome.test.utils.DynamoDBLocal

class ScanamoQueryStreamTest
    extends FunSpec
    with BeforeAndAfterEach
    with Eventually
    with IntegrationPatience
    with DynamoDBLocal
    with Matchers {


  // TODO: Find a better way of doing this!!
  val bigString = (1 to 400).map(_ => "a").foldLeft("")(_ + _)

  it("run a given function for each row") {

    (1 to 10)
      .map(
        i =>
          MiroTransformable(
            MiroID = s"Image$i",
            MiroCollection = "Collection",
            data = bigString,
            ReindexVersion = 1
        ))
      .par.foreach(
        Scanamo.put(dynamoDbClient)(miroDataTableName)
      )

    val result = run

    val currentState = Scanamo.scan[MiroTransformable](dynamoDbClient)("MiroData")

    println(result)
    println(currentState)

    true shouldBe false
  }



  def run: List[Either[DynamoReadError, MiroTransformable]] = {

    val scanamoQueryRequest = ScanamoQueryRequest(
      miroDataTableName,
      Some("ReindexTracker"),
      Query(
        AndQueryCondition(
          KeyEquals('ReindexShard, "default"),
          KeyIs('ReindexVersion, LT, 10)
        )),
      ScanamoQueryOptions.default
    )

    def updateVersion(
      resultGroup: List[Either[DynamoReadError, MiroTransformable]])
      : List[Either[DynamoReadError, MiroTransformable]] = {

      resultGroup.map {
        case Left(e) => Left(e)
        case Right(miroTransformable) => {
          val reindexItem = miroTransformable.getReindexItem

          Scanamo.update[MiroTransformable](dynamoDbClient)("MiroData")(
            reindexItem.hashKey and reindexItem.rangeKey,
            set('ReindexVersion -> (reindexItem.ReindexVersion + 1)))

          Right(miroTransformable)
        }
      }

    }

    val ops = ScanamoQueryStream
      .run[MiroTransformable, Either[DynamoReadError, MiroTransformable]](
        scanamoQueryRequest,
        updateVersion)

    Scanamo.exec(dynamoDbClient)(ops)
  }

}
