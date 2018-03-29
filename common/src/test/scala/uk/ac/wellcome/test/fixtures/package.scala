package uk.ac.wellcome.test

package object fixtures extends FixtureComposers {

  type TestWith[T, R] = T => R

  type Fixture[L, R] = TestWith[L, R] => R

  private val noop = (x: Any) => ()

  def fixture[L, R](create: => L, destroy: L => Unit = noop): Fixture[L, R] =
    (testWith: TestWith[L, R]) => {
      val loan = create
      try {
        testWith(loan)
      } finally {
        destroy(loan)
      }
    }

  implicit class ComposableFixture[L1, R](
    private val thisFixture: Fixture[L1, R]) {

    def and[T, L2](that: T)(
      implicit fc: FixtureComposer[L1, L2, T, R]): Fixture[L2, R] =
      fc.compose(thisFixture, that)

  }

}
