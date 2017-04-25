package uk.ac.wellcome.platform.api

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class ApiWorksTest extends FeatureTest {
  override def server = new EmbeddedHttpServer(new Server)

  test("it should return a list of works") {

  }
}
