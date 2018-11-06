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
      get(path).getOrElse {
        throw new RuntimeException(s"No value found for path $path")
      }

    def getOrElse[T](path: String)(default: T): T =
      get(path).getOrElse(default)
  }
}
