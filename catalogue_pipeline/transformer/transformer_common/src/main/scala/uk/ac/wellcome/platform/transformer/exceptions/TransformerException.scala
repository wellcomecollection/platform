package uk.ac.wellcome.platform.transformer.exceptions

import uk.ac.wellcome.exceptions.GracefulFailureException

case class TransformerException(e: Throwable) extends GracefulFailureException

case object TransformerException {
  def apply(message: String): TransformerException =
    TransformerException(new RuntimeException(message))
}
