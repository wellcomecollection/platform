package uk.ac.wellcome.platform.archive.common.config.builders

import com.typesafe.config.Config

object EnrichConfig {
  implicit class RichConfig(val underlying: Config) extends AnyVal {
    def get[T](path: String): Option[T] = {
      // Sometimes we may get a path that features two double dots, if there's an
      // empty namespace -- in this case, elide the two dots into one.
      val configPath = path.replaceAllLiterally("..", ".")

      if (underlying.hasPath(configPath)) {
        Some(underlying.getAnyRef(configPath).asInstanceOf[T])
      } else {
        None
      }
    }

    def required[T](path: String): T =
      get(path).getOrElse {

        // For some reason merely throwing an exception here doesn't cause the
        // app to exit, so a config failure just sits.  This causes a config
        // error to crash the app.
        println(s"No value found for path $path")
        System.exit(1)
        throw new RuntimeException(s"No value found for path $path")
      }

    def getOrElse[T](path: String)(default: T): T =
      get(path).getOrElse(default)
  }
}
