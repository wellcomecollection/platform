package uk.ac.wellcome.platform.archive.common.fixtures

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}

trait HttpFixtures extends Akka with ScalaFutures {
  def whenRequestReady[R](r: HttpRequest)(testWith: TestWith[HttpResponse, R]): R =
    withActorSystem { implicit actorSystem =>
      val request = Http().singleRequest(r)
      whenReady(request) { (response: HttpResponse) =>
        testWith(response)
      }
    }
}
