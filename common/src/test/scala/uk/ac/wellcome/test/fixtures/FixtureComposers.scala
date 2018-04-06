package uk.ac.wellcome.test.fixtures

trait FixtureComposers {

  implicit def NoArgsFixtureComposer[L1, L2, R] =
    new FixtureComposer[L1, (L1, L2), Fixture[L2, R], R] {

      override def compose(fixture1: Fixture[L1, R],
                           fixture2: Fixture[L2, R]): Fixture[(L1, L2), R] =
        (testWith: TestWith[(L1, L2), R]) =>
          fixture1 { loan1 =>
            fixture2 { loan2 =>
              testWith((loan1, loan2))
            }
        }

    }

  implicit def OneArgFixtureComposer[L1, L2, R] =
    new FixtureComposer[L1, (L1, L2), (L1) => Fixture[L2, R], R] {

      override def compose(
        fixture1: Fixture[L1, R],
        function: (L1) => Fixture[L2, R]): Fixture[(L1, L2), R] =
        (testWith: TestWith[(L1, L2), R]) =>
          fixture1 { loan1 =>
            val fixture2 = function(loan1)

            fixture2 { loan2 =>
              testWith((loan1, loan2))
            }
        }

    }

  implicit def TwoArgFixtureComposer[L1, L2, L3, R] =
    new FixtureComposer[(L1, L2), (L1, L2, L3), (L1, L2) => Fixture[L3, R], R] {

      override def compose(
        fixture1: Fixture[(L1, L2), R],
        function: (L1, L2) => Fixture[L3, R]): Fixture[(L1, L2, L3), R] =
        (testWith: TestWith[(L1, L2, L3), R]) =>
          fixture1 {
            case (loan1, loan2) =>
              val fixture2 = function(loan1, loan2)

              fixture2 { loan3 =>
                testWith((loan1, loan2, loan3))
              }
        }

    }

  implicit def ThreeArgFixtureComposer[L1, L2, L3, L4, R] =
    new FixtureComposer[
      ((L1, L2), L3),
      (L1, L2, L3, L4),
      (L1, L2, L3) => Fixture[L4, R],
      R] {

      override def compose(fixture1: Fixture[((L1, L2), L3), R],
                           function: (L1, L2, L3) => Fixture[L4, R])
        : Fixture[(L1, L2, L3, L4), R] =
        (testWith: TestWith[(L1, L2, L3, L4), R]) =>
          fixture1 {
            case ((loan1, loan2), loan3) =>
              val fixture2 = function(loan1, loan2, loan3)

              fixture2 { loan4 =>
                testWith((loan1, loan2, loan3, loan4))
              }
        }

    }

  implicit def XThreeArgFixtureComposer[L1, L2, L3, L4, R] =
    new FixtureComposer[
      (L1, L2, L3),
      (L1, L2, L3, L4),
      (L1, L2, L3) => Fixture[L4, R],
      R] {

      override def compose(fixture1: Fixture[(L1, L2, L3), R],
                           function: (L1, L2, L3) => Fixture[L4, R])
        : Fixture[(L1, L2, L3, L4), R] =
        (testWith: TestWith[(L1, L2, L3, L4), R]) =>
          fixture1 {
            case (loan1, loan2, loan3) =>
              val fixture2 = function(loan1, loan2, loan3)

              fixture2 { loan4 =>
                testWith((loan1, loan2, loan3, loan4))
              }
        }

    }

}
