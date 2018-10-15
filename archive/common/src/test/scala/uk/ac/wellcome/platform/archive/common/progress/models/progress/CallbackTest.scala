package uk.ac.wellcome.platform.archive.common.progress.models.progress

import java.net.URI

import org.scalatest.{FunSpec, Matchers}

class CallbackTest extends FunSpec with Matchers {

  it("is initialised with status pending") {
    val callback =
      Callback(new URI("http://www.wellcomecollection.org/callback/ok"))

    callback.callbackStatus shouldBe Callback.Pending
  }

  it("parses pending status") {
    val callbackStatus: Callback.Status = Callback.parseStatus("pending")

    callbackStatus shouldBe Callback.Pending
  }

  it("parses succeeded status") {
    val callbackStatus: Callback.Status = Callback.parseStatus("succeeded")

    callbackStatus shouldBe Callback.Succeeded
  }

  it("parses failed status") {
    val callbackStatus: Callback.Status = Callback.parseStatus("failed")

    callbackStatus shouldBe Callback.Failed
  }

  it("throws if there is a parse error") {
    a[MatchError] should be thrownBy Callback.parseStatus("error")
  }

}
