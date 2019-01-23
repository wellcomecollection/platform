package uk.ac.wellcome.platform.idminter.exceptions

case class IdMinterException(e: Throwable) extends Exception(e.getMessage)
