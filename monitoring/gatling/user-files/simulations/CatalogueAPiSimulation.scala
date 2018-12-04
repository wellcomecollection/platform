package computerdatabase

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class CatalogueAPiSimulation extends Simulation {

  val httpConf = http.baseUrl("https://api-stage.wellcomecollection.org/catalogue/v2/works")

  // This was taken from https://github.com/dwyl/english-words
  // and copied in the user-files/resources. I'm not committing
  // it because it's about 4MB
  val wordsFeeder = csv("words.csv").random()

  val scn = scenario("BasicSimulation")
    .feed(wordsFeeder)
    .exec(http("all_works").get("/"))
    .exec(http("search").get("/")
      .queryParam("query", "${word}"))
    .repeat(5)(feed(wordsFeeder).exec(http("search images").get("/")
      .queryParam("workType","q,k")
      .queryParam("items.locations.locationType","iiif-image")
      .queryParam("query","${word}")
      .queryParam("pageSize","100")))

  setUp( // 11
    scn.inject(constantUsersPerSec(2) during (20 minutes)) // 12
  ).protocols(httpConf) // 13
}