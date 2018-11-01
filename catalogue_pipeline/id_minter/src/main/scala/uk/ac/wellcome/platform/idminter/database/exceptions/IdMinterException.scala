package uk.ac.wellcome.platform.idminter.database.exceptions

import uk.ac.wellcome.exceptions.GracefulFailureException

case class IdMinterException(e: Throwable) extends Exception(e.getMessage) with GracefulFailureException
