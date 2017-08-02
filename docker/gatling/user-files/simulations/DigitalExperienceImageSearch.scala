package testing.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class DigitalExperienceImageSearch extends Simulation {

  val httpConf = http
    .baseURL("https://next.wellcomecollection.org/search")

  val searchFeeder = csv("search.csv").random

  val searchScn = scenario("article-full-size")
      .feed(searchFeeder)
      .exec(http("simple-search")
        .get("?query=${searchTerm}")
        .check(status.in(200, 304))
      )

  setUp(
    searchScn.inject(constantUsersPerSec(30) during(120 seconds) randomized)
  ).protocols(
    httpConf
  ).assertions(
    global.responseTime.max.lt(2000),
    global.successfulRequests.percent.gt(99)
  )
}
