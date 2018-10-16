package uk.ac.wellcome.platform.archive.common.progress.models.progress

case class FailedEvent[T](e: Throwable, t: T)
