package uk.ac.wellcome.platform.archiver

import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}

import scala.util.Random

object BagItUtils {

  def randomAlphanumeric(length: Int = 8) = {
    Random.alphanumeric take length mkString
  }

  def createDigest(string: String) = MessageDigest
    .getInstance("MD5")
    .digest(string.getBytes)
    .map(0xFF & _)
    .map {
      "%02x".format(_)
    }.foldLeft("") {
    _ + _
  }

  def createZip(files: List[FileEntry]) = {

    val zipFileName = s"/Users/k/Desktop/${randomAlphanumeric()}.zip"
    val zipFileOutputStream = new FileOutputStream(zipFileName)
    val zipOutputStream = new ZipOutputStream(zipFileOutputStream)

    files.foreach {
      case FileEntry(name, contents) =>
        val zipEntry = new ZipEntry(name)

        zipOutputStream.putNextEntry(zipEntry)
        zipOutputStream.write(contents.getBytes)
        zipOutputStream.closeEntry()
    }

    zipOutputStream.close()

    val zipFile = new ZipFile(zipFileName)

    zipFile
  }

  def createBagItZip(bagName: String, dataFileCount: Int = 1, valid: Boolean = true) = {

    // Create data files
    val dataFiles = (1 to dataFileCount).map { _ =>
      val fileName = randomAlphanumeric()
      val filePath = s"$bagName/data/$fileName.txt"
      val fileContents = Random.nextString(256)

      FileEntry(filePath, fileContents)
    }

    // Create data manifest
    val dataManifest = FileEntry(s"$bagName/manifest-md5.txt", dataFiles.map {
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
    }.mkString("\n"))

    // Create bagIt file
    val bagItFileContents =
      """BagIt-Version: 0.97
        |Tag-File-Character-Encoding: UTF-8
      """.stripMargin.trim
    val bagItFile = FileEntry(s"$bagName/bagit.txt", bagItFileContents)

    // Create bagInfo file
    val dateFormat = new java.text.SimpleDateFormat("YYYY-MM-dd")
    val date = dateFormat.format(new java.util.Date())
    val bagInfoFileContents =
      s"""Payload-Oxum: 61798.84
        |Bagging-Date: $date
        |Bag-Size: 60.5 KB
      """.stripMargin.trim
    val bagInfoFile = FileEntry(s"$bagName/bag-info.txt", bagInfoFileContents)

    // Create meta manifest
    val dataManifestChecksum = createDigest(dataManifest.contents)
    val bagItFileChecksum = createDigest(bagItFileContents)
    val bagInfoFileChecksum = createDigest(bagInfoFileContents)

    val metaManifest = FileEntry(
      s"$bagName/tagmanifest-md5.txt",
      s"""$dataManifestChecksum  manifest-md5.txt
         |$bagItFileChecksum  bagit.txt
         |$bagInfoFileChecksum  bag-info.txt
       """.stripMargin.trim
    )

    val allFiles =
      dataFiles ++ List(dataManifest, metaManifest, bagInfoFile, bagItFile)

    createZip(allFiles.toList)
  }
}

case class FileEntry(name: String, contents: String)
