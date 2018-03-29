package uk.ac.wellcome.test

package object fixtures extends FixtureComposers {

  type TestWith[T, R] = T => R

  type Fixture[L, R] = TestWith[L, R] => R

  implicit class ComposableFixture[L1, R](private val thisFixture: Fixture[L1, R]) {

    def and[T, L2](that: T)(implicit fc: FixtureComposer[L1, L2, T, R]): Fixture[L2, R] =
      fc.compose(thisFixture, that)

  }

}
