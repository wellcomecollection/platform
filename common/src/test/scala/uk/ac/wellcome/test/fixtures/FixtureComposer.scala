package uk.ac.wellcome.test.fixtures

trait FixtureComposer[L1, L2, T, R] {

  def compose(fixture: Fixture[L1, R], that: T): Fixture[L2, R]

}
