package uk.ac.wellcome.platform.reindex.reindex_worker.dynamo

import java.util

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import uk.ac.wellcome.storage.dynamo.TestVersioned

import scala.collection.JavaConverters._

trait ScanSpecScannerTestBase {
  protected def toAttributeMap(
    testV: TestVersioned): util.Map[String, AttributeValue] =
    Map(
      "id" -> new AttributeValue().withS(testV.id),
      "data" -> new AttributeValue().withS(testV.data),
      "version" -> new AttributeValue().withN(testV.version.toString)
    ).asJava
}
