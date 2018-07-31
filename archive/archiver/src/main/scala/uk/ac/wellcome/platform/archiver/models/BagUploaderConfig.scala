package uk.ac.wellcome.platform.archiver.models

case class BagUploaderConfig(
                              uploadNamespace: String,
                              uploadPrefix: String = "archive",
                              digestDelimiter: String = "  ",
                              digestNames: List[String] = List(
                                "manifest-md5.txt",
                                "tagmanifest-md5.txt"
                              )
                            )
