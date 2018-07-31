package uk.ac.wellcome.platform.archiver.models

import org.rogach.scallop.ScallopConf

class AppConfig(arguments: Seq[String]) extends ScallopConf(arguments) {

  val awsS3AccessKey = opt[String]()
  val awsS3SecretKey = opt[String]()

  val awsS3Region = opt[String](default = Some("eu-west-1"))
  val uploadNamespace = opt[String]()

  val uploadPrefix = opt[String](default = Some("archive"))
  val digestDelimiter = opt[String](default = Some("  "))

  verify()

  val digestNames: List[String] = List(
    "manifest-md5.txt",
    "tagmanifest-md5.txt"
  )
}
