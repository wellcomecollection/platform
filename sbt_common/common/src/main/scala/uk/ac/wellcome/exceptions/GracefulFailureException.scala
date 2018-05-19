package uk.ac.wellcome.exceptions

case class GracefulFailureException(e: Throwable)
    extends Exception(e.getMessage)
