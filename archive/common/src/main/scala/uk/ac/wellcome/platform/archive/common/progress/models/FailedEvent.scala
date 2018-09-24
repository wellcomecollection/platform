package uk.ac.wellcome.platform.archive.common.progress.models

case class FailedEvent[T](e: Throwable, t: T)
