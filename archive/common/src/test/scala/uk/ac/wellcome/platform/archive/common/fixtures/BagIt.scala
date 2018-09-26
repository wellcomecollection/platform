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
                dataFileCount: Int = 1,
                createDigest: String => String = createValidDigest,
                createDataManifest: (BagPath, List[(String, String)]) => Option[FileEntry] = createValidDataManifest,
                createTagManifest: (BagPath, List[(String, String)]) => Option[FileEntry] = createValidTagManifest,
                createBagItFile: BagPath => Option[FileEntry] = createValidBagItFile) = {

    val dataFiles = createDataFiles(bagName, dataFileCount)
    val filesAndDigest = dataFiles.map{case FileEntry(fileName, contents) => (fileName, createDigest(contents))}.toList

    val dataManifest = createDataManifest(bagName, filesAndDigest)

    val maybeBagItFile = createBagItFile(bagName)

    val bagInfoFile = FileEntry(s"$bagName/bag-info.txt", bagInfoFileContents)

    val tagManifestFiles = dataManifest.toList ++ maybeBagItFile.toList ++ List(bagInfoFile)

    val tagManifestFileAndDigests = tagManifestFiles.map{case FileEntry(fileName, contents) => (fileName, createDigest(contents))}.toList
    val metaManifest = createTagManifest(bagName, tagManifestFileAndDigests)

    dataFiles  ++ tagManifestFiles ++ metaManifest.toList
  }

  def createValidBagItFile(bagName: BagPath) = Some(FileEntry(s"$bagName/bagit.txt", bagItFileContents))

  def dataManifestWithNonExistingFile(bagPath: BagPath, filesAndDigests: Seq[(String,String)]) = Some(FileEntry(
    name = s"$bagPath/manifest-sha256.txt",
    contents = {
      val validContent = filesAndDigests.map {
        case (existingFileName, validFileDigest) =>
        s"""$validFileDigest  ${existingFileName.replace(s"$bagPath/", "")}"""
      }
      (validContent :+ """1234567890qwer  this/does/not/exists.jpg""").mkString("\n")
    }
  ))

  def dataManifestWithWrongChecksum(bagPath: BagPath, filesAndDigests: List[(String,String)]) = Some(FileEntry(
    name = s"$bagPath/manifest-sha256.txt",
    contents = {
      filesAndDigests match {
        case (head: (String,String)) :: (list: List[(String,String)]) =>
          val (invalidChecksumFileName,_) = head
          val invalidChecksumManifestEntry = s"""badDigest  ${invalidChecksumFileName.replace(s"$bagPath/", "")}"""
          val validEntries = list.map {
            case (existingFileName, validFileDigest) =>
              s"""$validFileDigest  ${existingFileName.replace(s"$bagPath/", "")}"""
          }
          (invalidChecksumManifestEntry +: validEntries).mkString("\n")
        case _ => ""
      }
    }
  ))

  private def createValidManifestFile(bagName: BagPath, dataFiles: List[(String, String)], manifestFileName: String) =     Some(FileEntry(
    s"$bagName/$manifestFileName",
    dataFiles
      .map {
        case (fileName, contentsDigest) =>
          val digestFileName = fileName.replace(s"$bagName/", "")
          s"$contentsDigest  $digestFileName"
      }
      .mkString("\n")
  ))

  def createValidDataManifest(bagName: BagPath, dataFiles: List[(String, String)]) = createValidManifestFile(bagName, dataFiles, "manifest-sha256.txt")
  def createValidTagManifest(bagName: BagPath, dataFiles: List[(String, String)]) = createValidManifestFile(bagName, dataFiles, "tagmanifest-sha256.txt")

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
