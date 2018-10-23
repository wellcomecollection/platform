package uk.ac.wellcome.platform.archive.common.fixtures

import java.security.MessageDigest

import uk.ac.wellcome.platform.archive.common.models.{ExternalIdentifier, PayloadOxum, SourceOrganisation}

import scala.util.Random

trait BagIt extends RandomThings {
  private val bagItFileContents = {
    """BagIt-Version: 0.97
      |Tag-File-Character-Encoding: UTF-8
    """.stripMargin.trim
  }

  def createBag(
    bagIdentifier: ExternalIdentifier,
    dataFileCount: Int = 1,
    createDigest: String => String = createValidDigest,
    createDataManifest: List[(String, String)] => Option[FileEntry] =
      createValidDataManifest,
    createTagManifest: List[(String, String)] => Option[FileEntry] =
      createValidTagManifest,
    createBagItFile: => Option[FileEntry] = createValidBagItFile,
    createBagInfoFile: ExternalIdentifier => Option[FileEntry] =
      createValidBagInfoFile): Seq[FileEntry] = {

    val dataFiles = createDataFiles(dataFileCount)
    val filesAndDigest = dataFiles.map {
      case FileEntry(fileName, contents) => (fileName, createDigest(contents))
    }.toList

    val dataManifest = createDataManifest(filesAndDigest)

    val maybeBagItFile = createBagItFile

    val maybeBagInfoFile = createBagInfoFile(bagIdentifier)

    val tagManifestFiles = dataManifest.toList ++ maybeBagItFile.toList ++ maybeBagInfoFile.toList

    val tagManifestFileAndDigests = tagManifestFiles.map {
      case FileEntry(fileName, contents) => (fileName, createDigest(contents))
    }.toList
    val metaManifest = createTagManifest(tagManifestFileAndDigests)

    dataFiles ++ tagManifestFiles ++ metaManifest.toList
  }

  def createValidBagItFile =
    Some(FileEntry("bagit.txt", bagItFileContents))

  def createValidBagInfoFile(bagIdentifier: ExternalIdentifier) =
    Some(FileEntry(s"bag-info.txt", bagInfoFileContents(bagIdentifier, randomSourceOrganisation, randomPayloadOxum)))

  def dataManifestWithNonExistingFile(filesAndDigests: Seq[(String, String)]) =
    Some(
      FileEntry(
        name = "manifest-sha256.txt",
        contents = {
          val validContent = filesAndDigests.map {
            case (existingFileName, validFileDigest) =>
              s"""$validFileDigest  $existingFileName"""
          }
          (validContent :+ """1234567890qwer  this/does/not/exists.jpg""")
            .mkString("\n")
        }
      ))

  def dataManifestWithWrongChecksum(filesAndDigests: List[(String, String)]) =
    Some(
      FileEntry(
        name = s"manifest-sha256.txt",
        contents = {
          filesAndDigests match {
            case (head: (String, String)) :: (list: List[(String, String)]) =>
              val (invalidChecksumFileName, _) = head
              val invalidChecksumManifestEntry =
                s"""badDigest  $invalidChecksumFileName"""
              val validEntries = list.map {
                case (existingFileName, validFileDigest) =>
                  s"""$validFileDigest  $existingFileName"""
              }
              (invalidChecksumManifestEntry +: validEntries).mkString("\n")
            case _ => ""
          }
        }
      ))

  def createValidDataManifest(dataFiles: List[(String, String)]) =
    createValidManifestFile(dataFiles, "manifest-sha256.txt")

  def createValidTagManifest(dataFiles: List[(String, String)]) =
    createValidManifestFile(dataFiles, "tagmanifest-sha256.txt")

  private def createValidManifestFile(dataFiles: List[(String, String)],
                                      manifestFileName: String) =
    Some(
      FileEntry(
        s"$manifestFileName",
        dataFiles
          .map {
            case (fileName, contentsDigest) =>
              s"$contentsDigest  $fileName"
          }
          .mkString("\n")
      ))

  def bagInfoFileContents(bagIdentifier: ExternalIdentifier, sourceOrganisation: SourceOrganisation, payloadOxum: PayloadOxum) = {
    s"""Source-Organization: $sourceOrganisation
       |External-Identifier: $bagIdentifier
       |Payload-Oxum: ${payloadOxum.payloadBytes}.${payloadOxum.numberOfPayloadFiles}
      """.stripMargin.trim
  }

  private def createDataFiles(dataFileCount: Int) = {
    val subPathLength = Random.nextInt(3)
    val subPathDirectories = (0 to subPathLength).map { _ =>
      randomAlphanumeric()
    }
    val subPath = subPathDirectories.mkString("/")

    (1 to dataFileCount).map { _ =>
      val fileName = randomAlphanumeric()
      val filePath = s"data/$subPath/$fileName.txt"
      val fileContents = Random.nextString(256)
      FileEntry(filePath, fileContents)
    }
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
