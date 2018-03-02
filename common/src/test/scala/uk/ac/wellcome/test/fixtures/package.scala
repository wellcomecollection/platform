package uk.ac.wellcome.test

import org.scalatest.Outcome

package object fixtures {

  type TestWith[T] = T => Outcome

}
