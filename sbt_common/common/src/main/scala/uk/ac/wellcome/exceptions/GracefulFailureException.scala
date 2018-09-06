package uk.ac.wellcome.exceptions

trait GracefulFailureException extends Exception { self: Throwable =>
  val message: String = self.getMessage
}
