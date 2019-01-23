package uk.ac.wellcome.platform.matcher.exceptions

case class MatcherException(e: Throwable) extends Exception(e.getMessage)
