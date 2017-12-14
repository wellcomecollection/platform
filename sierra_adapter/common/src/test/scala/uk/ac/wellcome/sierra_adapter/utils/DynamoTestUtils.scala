package uk.ac.wellcome.sierra_adapter.utils

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.query.UniqueKey
import org.scalatest.Matchers
import org.scalatest.concurrent.Eventually
import uk.ac.wellcome.sierra_adapter.locals.DynamoDBLocal

trait DynamoTestUtils extends Matchers with Eventually {

  val tableName: String
  val dynamoDbClient: AmazonDynamoDB

  def dynamoQueryEqualsValue[T: DynamoFormat](key: UniqueKey[_])(
    expectedValue: T) = {

    println(s"Searching DynamoDB for expectedValue = $expectedValue")

    eventually {
      val actualValue = Scanamo.get[T](dynamoDbClient)(tableName)(key).get
      actualValue shouldEqual Right(expectedValue)
    }
  }
}