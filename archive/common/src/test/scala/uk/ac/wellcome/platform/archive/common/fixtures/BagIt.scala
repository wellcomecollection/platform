package uk.ac.wellcome.platform.archive.common.fixtures

import java.security.MessageDigest

import uk.ac.wellcome.platform.archive.common.models.BagPath

import scala.collection.immutable
import scala.util.Random

trait BagIt {
  def createValidBag(bagName: BagPath,
                     dataFileCount: Int = 1) = createBag(bagName, dataFileCount, valid = true)
  def createInvalidBag(bagName: BagPath,
                     dataFileCount: Int = 1) = createBag(bagName, dataFileCount, valid = false)

  private def createBag(bagName: BagPath,
                dataFileCount: Int = 1,
                valid: Boolean) = {

    val dataFiles = createDataFiles(s"$bagName/data", dataFileCount)
    val objectFiles = createDataFiles(s"$bagName/object", dataFileCount)

    val dataManifest = createDataManifest(bagName, dataFiles, objectFiles, valid)

    // Create bagIt file
    val bagItFileContents =
      createBagitContents
    val bagItFile = FileEntry(s"$bagName/bagit.txt", bagItFileContents)

    val bagInfoFileContents = createBagInfoContents
    val bagInfoFile = FileEntry(s"$bagName/bag-info.txt", bagInfoFileContents)

    // Create meta manifest
    val metaManifest = createTagManifest(bagName, dataManifest, bagItFileContents, bagInfoFileContents)

    dataFiles ++ objectFiles ++ List(
      dataManifest,
      metaManifest,
      bagInfoFile,
      bagItFile
    )
  }

  private def createBagInfoContents = {
    val date = new java.text.SimpleDateFormat("YYYY-MM-dd").format(new java.util.Date())
    s"""Payload-Oxum: 61798.84
       |Bagging-Date: $date

       |Bag-Size: 60.5 KB
      """.stripMargin.trim
  }

  private def createBagitContents = {
    """BagIt-Version: 0.97
      |Tag-File-Character-Encoding: UTF-8
    """.stripMargin.trim
  }

  private def createTagManifest(bagName: BagPath, dataManifest: FileEntry, bagItFileContents: String, bagInfoFileContents: String) = {
    val dataManifestCheckSum = createDigest(dataManifest.contents)
    val bagitFileCheckSum = createDigest(bagItFileContents)
    val bagInfoFileChecksum = createDigest(bagInfoFileContents)
    FileEntry(
      s"$bagName/tagmanifest-sha256.txt",
      s"""$dataManifestCheckSum  manifest-sha256.txt
         |$bagitFileCheckSum  bagit.txt
         |$bagInfoFileChecksum  bag-info.txt
       """.stripMargin.trim
    )
  }

  private def createDataManifest(bagName: BagPath, dataFiles: immutable.IndexedSeq[FileEntry], objectFiles: immutable.IndexedSeq[FileEntry], valid: Boolean) = {
    FileEntry(
      s"$bagName/manifest-sha256.txt",
      (dataFiles ++ objectFiles)
        .map {
          case FileEntry(fileName, fileContents) => {
            val fileContentsDigest = createDigest(fileContents)
            val contentsDigest = if (!valid) {
              "bad_digest"
            } else {
              fileContentsDigest
            }
            val digestFileName = fileName.replace(s"$bagName/", "")
            s"$contentsDigest  $digestFileName"
          }
        }
        .mkString("\n")
    )
  }

  private def createDataFiles(p: String, dataFileCount: Int) = {
    (1 to dataFileCount).map { _ =>
      val fileName = randomAlphanumeric()
      val filePath = s"$p/$fileName.txt"
      val fileContents = Random.nextString(256)
      FileEntry(filePath, fileContents)
    }
  }

  def randomAlphanumeric(length: Int = 8) = {
    Random.alphanumeric take length mkString
  }

  def createDigest(string: String) =
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
