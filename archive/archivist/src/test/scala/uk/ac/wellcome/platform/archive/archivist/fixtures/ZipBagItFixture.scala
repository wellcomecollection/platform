package uk.ac.wellcome.platform.archive.archivist.fixtures

import java.io.{File, FileOutputStream}
import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}

import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.fixtures.{BagIt, FileEntry}
import uk.ac.wellcome.test.fixtures.TestWith

trait ZipBagItFixture extends BagIt with Logging {

  def withZipFile[R](files: Seq[FileEntry])(testWith: TestWith[ZipFile, R]) = {
    val file = File.createTempFile(randomAlphanumeric(), ".zip")
    val zipFileOutputStream = new FileOutputStream(file)
    val zipOutputStream = new ZipOutputStream(zipFileOutputStream)
    files.foreach {
      case FileEntry(name, contents) =>
        info(s"Adding $name to zip contents")
        val zipEntry = new ZipEntry(name)
        zipOutputStream.putNextEntry(zipEntry)
        zipOutputStream.write(contents.getBytes)
        zipOutputStream.closeEntry()
    }
    zipOutputStream.close()
    val zipFile = new ZipFile(file)

    info(s"zipfile full path: ${file.getAbsolutePath}")
    testWith(zipFile)
    file.delete()
  }

  def withBagItZip[R](
    bagIdentifier: String = randomAlphanumeric(),
    dataFileCount: Int = 1,
    createDigest: String => String = createValidDigest,
    createDataManifest: List[(String, String)] => Option[FileEntry] =
      createValidDataManifest,
    createTagManifest: List[(String, String)] => Option[FileEntry] =
      createValidTagManifest,
    createBagItFile: => Option[FileEntry] = createValidBagItFile,
    createBagInfoFile: String => Option[FileEntry] = createValidBagInfoFile
  )(testWith: TestWith[(String, ZipFile), R]) = {

    info(s"Creating bag $bagIdentifier")

    val allFiles = createBag(
      bagIdentifier,
      dataFileCount,
      createDigest = createDigest,
      createDataManifest = createDataManifest,
      createTagManifest = createTagManifest,
      createBagItFile = createBagItFile,
      createBagInfoFile = createBagInfoFile
    )
    info(s"Adding files $allFiles to bag $bagIdentifier")
    withZipFile(allFiles) { zipFile =>
      testWith((bagIdentifier, zipFile))
    }
  }
}
