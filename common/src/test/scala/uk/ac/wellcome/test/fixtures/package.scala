package uk.ac.wellcome.test

package object fixtures {

  type TestWith[T, R] = T => R

  type Fixture[T, R] = () => TestWith[T, R] => R

  def composeFixtures[T1, T2, R](fixture1: Fixture[T1, R], fixture2: Fixture[T2, R]): Fixture[(T1, T2), R] =
    ???

}
