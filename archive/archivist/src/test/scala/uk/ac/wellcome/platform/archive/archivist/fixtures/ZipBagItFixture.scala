package uk.ac.wellcome.platform.archive.archivist.fixtures

import java.io.{File, FileOutputStream}
import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}

import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.fixtures.{BagIt, FileEntry}
import uk.ac.wellcome.platform.archive.common.models.BagPath
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
                       bagName: BagPath = BagPath(randomAlphanumeric()),
                       dataFileCount: Int = 1,
                       createDigest: String => String = createValidDigest,
                       createDataManifest: (BagPath, List[(String,String)]) => Option[FileEntry]= createValidDataManifest,
                       createTagManifest: (BagPath, List[(String,String)]) => Option[FileEntry]= createValidTagManifest,
                       createBagItFile: BagPath => Option[FileEntry] = createValidBagItFile
                     )(
    testWith: TestWith[(BagPath, ZipFile), R]) = {

    info(s"Creating bag $bagName")

    val allFiles = createBag(bagName, dataFileCount, createDigest = createDigest, createDataManifest = createDataManifest, createTagManifest = createTagManifest, createBagItFile = createBagItFile)
    info(s"Adding files $allFiles to bag $bagName")
    withZipFile(allFiles) { zipFile =>
      testWith((bagName, zipFile))
    }
  }
}
