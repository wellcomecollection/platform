package uk.ac.wellcome.platform.transformer.sierra.exceptions

import uk.ac.wellcome.exceptions.GracefulFailureException

case class SierraTransformerException(e: Throwable)
  extends Exception(e.getMessage)
    with GracefulFailureException

case object SierraTransformerException {
  def apply(message: String): SierraTransformerException =
    SierraTransformerException(new RuntimeException(message))
}
