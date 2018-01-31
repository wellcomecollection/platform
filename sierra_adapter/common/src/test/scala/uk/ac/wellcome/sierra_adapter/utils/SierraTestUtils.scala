package uk.ac.wellcome.sierra_adapter.utils

import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.query.UniqueKey
import org.scalatest.{Matchers, Suite}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.models.VersionUpdater

trait SierraTestUtils
    extends Matchers
    with Eventually
    with ScalaFutures
    with MockitoSugar
    with ExtendedPatience { this: Suite =>
}
