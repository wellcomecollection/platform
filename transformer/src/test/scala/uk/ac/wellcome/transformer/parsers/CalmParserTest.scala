package uk.ac.wellcome.transformer.parsers

import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.fasterxml.jackson.databind.JsonMappingException
import org.scalatest.{FunSpec, FunSuite}
import uk.ac.wellcome.models.{SourceIdentifier, UnifiedItem}
import uk.ac.wellcome.transformer.receive.RecordReceiver

class CalmParserTest extends FunSpec {


//  it("should return a failed future if it's unable to transform the parsed record") {
//    val recordReceiver = new RecordReceiver(mockSNSWriter, mockTransformableParser(mockRecord, UnifiedItem(List(SourceIdentifier("Calm", "AltRefNo", "1234")), None)))
//    val future = recordReceiver.receiveRecord(
//      new RecordAdapter(createNonTransformableRecord))
//
//    whenReady(future.failed) { x =>
//      x shouldBe a[JsonMappingException]
//    }
//  }
}
