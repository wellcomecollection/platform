package uk.ac.wellcome.platform.sierra_dynamo.api

import akka.NotUsed
import akka.stream.ThrottleMode
import akka.stream.scaladsl.Source
import io.circe.Json

import scala.concurrent.duration._

case class ThrottleRate(elements: Int, per: FiniteDuration, maximumBurst: Int)
case object ThrottleRate {
  def apply(elements: Int, per: FiniteDuration): ThrottleRate = ThrottleRate(elements, per, 0)
}

object SierraSource {


  def apply(apiUrl: String, oauthKey: String, oauthSecret: String, throttleRate: ThrottleRate)(
    resourceType: String,
    params: Map[String, String]): Source[Json, NotUsed] = {

    Source.fromGraph(
      new SierraPageSource(apiUrl, oauthKey, oauthSecret)(resourceType, params)
    ).throttle(
      throttleRate.elements,
      throttleRate.per,
      throttleRate.maximumBurst,
      ThrottleMode.shaping
    ).mapConcat(identity)
  }

  def apply(apiUrl: String, oauthKey: String, oauthSecret: String)(
    resourceType: String,
    params: Map[String, String]): Source[Json, NotUsed] = {

    Source.fromGraph(
      new SierraPageSource(apiUrl, oauthKey, oauthSecret)(resourceType, params)
    ).mapConcat(identity)
  }

}