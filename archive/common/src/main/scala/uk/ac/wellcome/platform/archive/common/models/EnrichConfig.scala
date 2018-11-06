package uk.ac.wellcome.platform.archive.common.models

import com.typesafe.config.Config

object EnrichConfig {
  implicit class RichConfig(val underlying: Config) extends AnyVal {
    def get[T](path: String): Option[T] =
      if (underlying.hasPath(path)) {
        Some(underlying.getAnyRef(path).asInstanceOf[T])
      } else {
        None
      }

    def required[T](path: String): T =
      get(path).get

    def getOrElse[T](path: String)(default: T): T =
      get(path) match {
        case Some(t) => t
        case None => default
      }
  }
}
