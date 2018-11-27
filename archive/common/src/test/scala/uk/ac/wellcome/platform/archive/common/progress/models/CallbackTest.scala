package uk.ac.wellcome.platform.archive.common.progress.models

import java.net.URI

import org.scalatest.{FunSpec, Matchers}

class CallbackTest extends FunSpec with Matchers {
  val callbackUri = new URI("http://www.wellcomecollection.org/callback/ok")

  it("is created with status pending") {
    val callback = Callback(callbackUri)

    callback.status shouldBe Callback.Pending
  }

  import org.scalatest.prop.TableDrivenPropertyChecks._
  private val callbackStatus = Table(
    ("string-status", "parsed-status"),
    ("processing", Callback.Pending),
    ("succeeded", Callback.Succeeded),
    ("failed", Callback.Failed),
  )

  it("converts all callback status values to strings") {
    forAll(callbackStatus) { (statusString, status) =>
      Callback(callbackUri, status).status.toString shouldBe statusString
    }
  }

}
