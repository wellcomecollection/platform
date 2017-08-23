package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}

import uk.ac.wellcome.platform.idminter.utils.IdentifiableWalker


/** Tests that the Miro transformer extracts the "title" field correctly.
 *
 *  The rules around this heuristic are somewhat fiddly, and we need to be
 *  careful that we're extracting the right fields from the Miro metadata.
 */
class IdentifiableWalkerTest
    extends FunSpec
    with Matchers {

  it("should find nothing for an empty map") {
    IdentifiableWalker
  }
}
