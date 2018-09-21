package uk.ac.wellcome.platform.archive.common.fixtures

import java.security.MessageDigest

import uk.ac.wellcome.platform.archive.common.models.BagPath

import scala.util.Random

trait BagIt {
  private val bagItFileContents = {
    """BagIt-Version: 0.97
      |Tag-File-Character-Encoding: UTF-8
    """.stripMargin.trim
  }
  private val bagInfoFileContents = {
    val date = new java.text.SimpleDateFormat("YYYY-MM-dd").format(new java.util.Date())
    s"""Payload-Oxum: 61798.84
       |Bagging-Date: $date

       |Bag-Size: 60.5 KB
      """.stripMargin.trim
  }

  def createBag(bagName: BagPath,
                dataFileCount: Int = 1, createDigest: String => String = createValidDigest, createDataManifest: (BagPath, Seq[(String, String)]) => FileEntry = createValidDataManifest) = {

    val dataFiles = createDataFiles(bagName, dataFileCount)
    val filesAndDigest = dataFiles.map{case FileEntry(fileName, contents) => (fileName, createDigest(contents))}

    val dataManifest = createDataManifest(bagName, filesAndDigest)

    val bagItFile = FileEntry(s"$bagName/bagit.txt", bagItFileContents)

    val bagInfoFile = FileEntry(s"$bagName/bag-info.txt", bagInfoFileContents)

    val metaManifest = createTagManifest(bagName, dataManifest, bagItFileContents, bagInfoFileContents)

    dataFiles  ++ List(
      dataManifest,
      metaManifest,
      bagInfoFile,
      bagItFile
    )
  }

  private def createTagManifest(bagName: BagPath, dataManifest: FileEntry, bagItFileContents: String, bagInfoFileContents: String) = {
    val dataManifestCheckSum = createValidDigest(dataManifest.contents)
    val bagitFileCheckSum = createValidDigest(bagItFileContents)
    val bagInfoFileChecksum = createValidDigest(bagInfoFileContents)
    FileEntry(
      s"$bagName/tagmanifest-sha256.txt",
      s"""$dataManifestCheckSum  manifest-sha256.txt
         |$bagitFileCheckSum  bagit.txt
         |$bagInfoFileChecksum  bag-info.txt
       """.stripMargin.trim
    )
  }

  def createValidDataManifest(bagName: BagPath, dataFiles: Seq[(String, String)]) = {
    FileEntry(
      s"$bagName/manifest-sha256.txt",
      dataFiles
        .map {
          case (fileName, contentsDigest) =>
            val digestFileName = fileName.replace(s"$bagName/", "")
            s"$contentsDigest  $digestFileName"
        }
        .mkString("\n")
    )
  }

  private def createDataFiles(bagPath: BagPath, dataFileCount: Int) = {
    val subPathLength = Random.nextInt(3)
    val subPathDirectories = (0 to subPathLength).map {_ =>
      randomAlphanumeric()
    }
    val subPath = subPathDirectories.mkString("/")

    (1 to dataFileCount).map { _ =>
      val fileName = randomAlphanumeric()
      val filePath = s"$bagPath/$subPath/$fileName.txt"
      val fileContents = Random.nextString(256)
      FileEntry(filePath, fileContents)
    }
  }

  def randomAlphanumeric(length: Int = 8) = {
    Random.alphanumeric take length mkString
  }

  def createValidDigest(string: String) =
    MessageDigest
      .getInstance("SHA-256")
      .digest(string.getBytes)
      .map(0xFF & _)
      .map {
        "%02x".format(_)
      }
      .foldLeft("") {
        _ + _
      }
}

case class FileEntry(name: String, contents: String)
