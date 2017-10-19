package testing.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class LorisSimulation extends Simulation {

  val defaultUseCloudFront = false
  val defaultImagesPerArticle = 15
  val defaultImagesPerSearch = 20
  val defaultUsersToSimulate = 5

  val useCloudFront = sys.env.get("USE_CLOUDFRONT").map(_.toBoolean)
    .getOrElse(defaultUseCloudFront)

  val imagesPerArticle = sys.env.get("IMAGES_PER_ARTICLE").map(_.toInt)
    .getOrElse(defaultImagesPerArticle)

  val imagesPerSearch = sys.env.get("IMAGES_PER_SEARCH").map(_.toInt)
    .getOrElse(defaultImagesPerSearch)

  val usersToSimulate = sys.env.get("USERS_TO_SIMULATE").map(_.toInt)
      .getOrElse(defaultUsersToSimulate)

  val hostname = if (useCloudFront) "iiif" else "iiif-origin"

  val httpConf = http
    .baseURL(s"https://$hostname.wellcomecollection.org")

  // We expect a small subset of images to be requested repeatedly -- for
  // example, images that are embedded in an Explore article.  These images
  // are likely to be kept in Loris's cache –– request them at thumbnail
  // and full-size, repeatedly.
  // TODO: multiple images at once
  val articleFeeder = csv("article.csv").random
  val articleScn = scenario("article-full-size")
    .repeat(imagesPerArticle) {
      feed(articleFeeder)
        .exec(http("article-thumbnail")
          .get("/image/${ident}/full/200,/0/default.jpg")
          .check(status.in(200, 304))
        )
        .exec(http("article-full-size")
          .get("/image/${ident}/full/full/0/default.jpg")
          .check(status.in(200, 304))
        )
    }

  // Now ask for a wider variety of idents, at full-size.  For example,
  // somebody browsing the catalogue.
  val identFeeder = csv("ident.csv").random

  val randomAccessScn = scenario("random-full-size")
    .feed(identFeeder)
    .exec(http("random-full-size")
      .get("/image/${ident}/full/full/0/default.jpg")
      .check(status.in(200, 304))
    )

  // Somebody fetching lots of thumbnails for a search page
  val searchPageScn = scenario("search-thumbnail")
    .repeat(imagesPerSearch) {
      feed(identFeeder)
        .exec(http("search-thumbnail")
          .get("/image/${ident}/full/200,/0/default.jpg")
          .check(status.in(200, 304))
        )
    }

  val regionFeeder = csv("region.csv").random
  val sizeFeeder = csv("size.csv").random
  val rotationFeeder = csv("rotate.csv").random

  // And a handful of requests that involve more image processing
  val complexScn = scenario("complex")
    .feed(identFeeder)
    .feed(regionFeeder)
    .feed(sizeFeeder)
    .feed(rotationFeeder)
    .exec(http("complex-scenario")
      .get("/image/${ident}/${region}/${size}/${rotate}/default.jpg")
      .check(status.in(200, 304))
    )

  setUp(
    articleScn.inject(rampUsers(usersToSimulate) over (5 seconds)),
    randomAccessScn.inject(rampUsers(usersToSimulate) over (5 seconds)),
    searchPageScn.inject(rampUsers(usersToSimulate) over (5 seconds)),
    complexScn.inject(rampUsers(usersToSimulate / 2) over (3 seconds))
  ).protocols(
    httpConf
  ).assertions(
    global.responseTime.percentile3.lt(1500),  // 95th percentile
    global.responseTime.percentile4.lt(2000),  // 99th percentile
    global.successfulRequests.percent.gt(99)
  )
}
