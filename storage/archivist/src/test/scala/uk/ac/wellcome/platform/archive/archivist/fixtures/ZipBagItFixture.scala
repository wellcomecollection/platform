package uk.ac.wellcome.platform.archive.archivist.fixtures

import java.io.{File, FileOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}

import grizzled.slf4j.Logging
import uk.ac.wellcome.fixtures._
import uk.ac.wellcome.platform.archive.common.fixtures.{BagIt, FileEntry}
import uk.ac.wellcome.platform.archive.common.models.bagit.BagInfo

trait ZipBagItFixture extends BagIt with Logging {

  def withZipFile[R](files: Seq[FileEntry]): Fixture[File, R] =
    fixture[File, R](
      create = {
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

        info(s"zipfile full path: ${file.getAbsolutePath}")
        file
      },
      destroy = { _.delete() }
    )

  def withBagItZip[R](
    bagInfo: BagInfo = randomBagInfo,
    dataFileCount: Int = 1,
    createDigest: String => String = createValidDigest,
    createDataManifest: List[(String, String)] => Option[FileEntry] =
      createValidDataManifest,
    createTagManifest: List[(String, String)] => Option[FileEntry] =
      createValidTagManifest,
    createBagItFile: => Option[FileEntry] = createValidBagItFile,
    createBagInfoFile: BagInfo => Option[FileEntry] = createValidBagInfoFile
  )(testWith: TestWith[File, R]): R = {
    info(s"Creating bag ${bagInfo.externalIdentifier}")

    val allFiles = createBag(
      bagInfo,
      dataFileCount,
      createDigest = createDigest,
      createDataManifest = createDataManifest,
      createTagManifest = createTagManifest,
      createBagItFile = createBagItFile,
      createBagInfoFile = createBagInfoFile
    )
    info(s"Adding files $allFiles to bag ${bagInfo.externalIdentifier}")
    withZipFile(allFiles) { zipFile =>
      testWith(zipFile)
    }
  }
}
