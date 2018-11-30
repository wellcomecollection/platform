package uk.ac.wellcome.platform.transformer.miro.exceptions

import uk.ac.wellcome.exceptions.GracefulFailureException

case class MiroTransformerException(e: Throwable)
    extends Exception(e.getMessage)
    with GracefulFailureException

case object MiroTransformerException {
  def apply(message: String): MiroTransformerException =
    MiroTransformerException(new RuntimeException(message))
}
