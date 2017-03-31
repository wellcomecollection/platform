import sbt._
import Keys._

import sbtecr.EcrPlugin.autoImport._
import sbtbuildenv.BuildEnvPlugin.autoImport._
import sbtconfigs3javaopts.ConfigS3JavaOptsPlugin.autoImport._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import com.amazonaws.regions.{Region, Regions}
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._

object Packager {
  val settings: Seq[Def.Setting[_]] = Seq(
    dockerBaseImage := "anapsix/alpine-java",
    region in ecr := Region.getRegion(Regions.EU_WEST_1),
    repositoryName in ecr :=
      s"${organization.value}/${name.value}:${version.value}_${buildEnv.value}",
    localDockerImage in ecr := s"${name.value}:${version.value}",
    push in ecr :=
      (push in ecr dependsOn (publishLocal in Docker, login in ecr)).value,
    configS3Bucket in configS3JavaOpts := sys.props
      .get("configBucket")
      .getOrElse("default"),
    configS3Stage in configS3JavaOpts := buildEnv.value.toString,
    configS3App in configS3JavaOpts := name.value.toString,
    javaOptions in Universal ++= configS3JavaOpts.value
  )
}
