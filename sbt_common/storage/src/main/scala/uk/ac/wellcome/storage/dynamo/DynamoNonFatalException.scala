package uk.ac.wellcome.storage.dynamo

case class DynamoNonFatalException(e: Throwable)
  extends Exception(e.getMessage)

