import sbt._
import Keys._

import sbtbuildenv.BuildEnvPlugin.autoImport._
import com.tapad.docker.DockerComposePlugin.autoImport._

object DockerCompose {
  val settings: Seq[Def.Setting[_]] = Seq(
    composeNoBuild := true
  )
}
