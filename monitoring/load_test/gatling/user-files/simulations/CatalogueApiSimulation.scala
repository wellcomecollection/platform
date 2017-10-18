package testing.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class CatalogueApiSimulation extends Simulation {

  val defaultUsersPerSec = 20
  val defaultDuration = 60

  val usersPerSec = sys.env.get("USERS_PER_SEC").map(_.toInt)
    .getOrElse(defaultUsersPerSec)

  val duration = sys.env.get("DEFAULT_DURATION").map(_.toInt)
    .getOrElse(defaultDuration)

  val httpConf = http
    .baseURL("https://api.wellcomecollection.org/catalogue/v1")
    .inferHtmlResources()

  val searchFeeder = csv("terms.csv").random

  val searchScn = scenario("simple-search")
    .feed(searchFeeder)
    .exec(http("works")
      .get("/works?query=${term}")
      .check(status.in(200, 304))
    )

  setUp(
    searchScn.inject(constantUsersPerSec(usersPerSec) during(duration seconds) randomized)
  ).protocols(
    httpConf
  ).assertions(
    global.responseTime.max.lt(500),
    global.successfulRequests.percent.gt(99)
  )
}
