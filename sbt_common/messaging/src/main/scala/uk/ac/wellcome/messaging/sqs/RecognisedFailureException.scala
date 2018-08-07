package uk.ac.wellcome.messaging.sqs

/** Should be thrown to indicate any exception which is "recognised" -- that
  * is, the cause is understood.
  *
  * Instances of this exception are counted as a separate metric.
  */
case class RecognisedFailureException(e: Throwable)
    extends Exception(e.getMessage)
