package uk.ac.wellcome.platform.archive.common.modules.config

import com.typesafe.config.Config

object ConfigHelper {
  implicit class RichConfig(val underlying: Config) extends AnyVal {
    def get[T](path: String): Option[T] = if (underlying.hasPath(path)) {
      Some(underlying.getAnyRef(path).asInstanceOf[T])
    } else {
      None
    }

    def required[T](path: String): T =
      underlying.getAnyRef(path).asInstanceOf[T]

    def getOrElse[T](path: String)(t: T): T =
      if (underlying.hasPath(path)) {
        underlying.getAnyRef(path).asInstanceOf[T]
      } else {
        t
      }
  }
}
