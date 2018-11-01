package uk.ac.wellcome.platform.matcher.exceptions

import uk.ac.wellcome.exceptions.GracefulFailureException

case class MatcherException(e: Throwable) extends Exception(e.getMessage) with GracefulFailureException
