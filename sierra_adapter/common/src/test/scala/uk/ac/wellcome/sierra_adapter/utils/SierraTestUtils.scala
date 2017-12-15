package uk.ac.wellcome.sierra_adapter.utils

import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.query.UniqueKey
import org.scalatest.{Matchers, Suite}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.sierra_adapter.locals.DynamoDBLocal
import uk.ac.wellcome.test.utils.ExtendedPatience

trait SierraTestUtils
    extends DynamoDBLocal
    with Matchers
    with Eventually
    with ScalaFutures
    with MockitoSugar
    with ExtendedPatience { this: Suite =>

  def dynamoQueryEqualsValue[T: DynamoFormat](key: UniqueKey[_])(
    expectedValue: T) = {

    println(s"Searching DynamoDB for expectedValue = $expectedValue")

    eventually {
      val actualValue = Scanamo.get[T](dynamoDbClient)(tableName)(key).get
      actualValue shouldEqual Right(expectedValue)
    }
  }
}
