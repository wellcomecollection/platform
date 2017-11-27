package uk.ac.wellcome.platform.sierra_to_dynamo.sink

import java.time.Instant

import akka.Done
import akka.stream.scaladsl.Sink
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.{Scanamo, Table}
import com.gu.scanamo.syntax._
import io.circe.Json
import io.circe.optics.JsonPath
import uk.ac.wellcome.platform.sierra_to_dynamo.models.SierraRecord
import uk.ac.wellcome.platform.sierra_to_dynamo.models.SierraRecord._

import scala.concurrent.Future

object SierraDynamoSink {
  def apply(client: AmazonDynamoDB,
            tableName: String): Sink[Json, Future[Done]] =
    Sink.foreach(json => {
      val record = SierraRecord(
        id = JsonPath.root.id.string.getOption(json).get,
        data = json.noSpaces,
        updatedDate =
          Instant.parse(JsonPath.root.updatedDate.string.getOption(json).get)
      )

      val table = Table[SierraRecord](tableName)
      val ops = table
        .given(
          not(attributeExists('id)) or
            (attributeExists('id) and 'updatedDate < record.updatedDate.getEpochSecond)
        )
        .put(record)
      Scanamo.exec(client)(ops)
    })
}
