package uk.ac.wellcome.platform.archive.common.progress.models.progress

import java.net.URI

import org.scalatest.{FunSpec, Matchers}

class CallbackTest extends FunSpec with Matchers {
  it("is initialised with status pending") {
    val callback =
      Callback(new URI("http://www.wellcomecollection.org/callback/ok"))

    callback.callbackStatus shouldBe Callback.Pending
  }

  import org.scalatest.prop.TableDrivenPropertyChecks._
  private val callbackStatus = Table(
    ("stringStatus", "parsedStatus"),
    ("pending", Callback.Pending),
    ("succeeded", Callback.Succeeded),
    ("failed", Callback.Failed),
  )
  it("parses all callback status") {
    forAll (callbackStatus) { (statusString, status) =>
      Callback.parseStatus(statusString)  shouldBe status
    }
  }

  it("throws if there is a parse error") {
    a [MatchError] should be thrownBy Callback.parseStatus("not-valid")
  }

}
