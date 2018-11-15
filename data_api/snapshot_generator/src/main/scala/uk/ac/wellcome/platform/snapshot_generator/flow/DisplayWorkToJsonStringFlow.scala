package uk.ac.wellcome.platform.snapshot_generator.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.fasterxml.jackson.databind.ObjectMapper
import uk.ac.wellcome.display.json.DisplayJsonUtil
import uk.ac.wellcome.display.json.DisplayJsonUtil._
import uk.ac.wellcome.display.models.DisplayWork

object DisplayWorkToJsonStringFlow {

  def apply(mapper: ObjectMapper): Flow[DisplayWork, String, NotUsed] =
    Flow.fromFunction({ work =>
      DisplayJsonUtil.toJson(work)
    })
}
