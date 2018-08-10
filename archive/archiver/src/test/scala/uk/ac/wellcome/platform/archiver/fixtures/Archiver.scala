package uk.ac.wellcome.platform.archiver.fixtures

import java.io.{File, FileOutputStream}
import java.nio.file.{Path, Paths}
import java.security.MessageDigest
import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}

import com.google.inject.Guice
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archiver.flow.BagName
import uk.ac.wellcome.platform.archiver.modules._
import uk.ac.wellcome.platform.archiver.{Archiver => ArchiverApp}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

import scala.util.Random

trait Archiver extends AkkaS3 with Messaging {

  def withBag[R](path: Path, ingestBucket: Bucket, queuePair: QueuePair)(
    testWith: TestWith[BagName, R]) = {
    val bagName = randomAlphanumeric()
    val uploadKey = s"upload/path/$bagName.zip"

    s3Client.putObject(ingestBucket.name, uploadKey, path.toFile)

    val uploadObjectLocation = ObjectLocation(ingestBucket.name, uploadKey)
    sendNotificationToSQS(queuePair.queue, uploadObjectLocation)

    info(s"Creating bag $bagName")

    testWith(BagName(bagName))
  }

  def withFakeBag[R](ingestBucket: Bucket,
                     queuePair: QueuePair,
                     valid: Boolean = true)(testWith: TestWith[BagName, R]) = {
    val bagName = randomAlphanumeric()
    val (zipFile, fileName) = createBagItZip(bagName, 12, valid)

    withBag(Paths.get(fileName), ingestBucket, queuePair) { bag =>
      testWith(bag)
    }
  }

  def withApp[R](storageBucket: Bucket, queuePair: QueuePair, topicArn: Topic)(
    testWith: TestWith[ArchiverApp, R]) = {
    val archiver = new ArchiverApp {
      val injector = Guice.createInjector(
        new TestAppConfigModule(
          queuePair.queue.url,
          storageBucket.name,
          topicArn.arn),
        ConfigModule,
        AkkaModule,
        AkkaS3ClientModule,
        CloudWatchClientModule,
        SQSClientModule
      )
    }
    testWith(archiver)
  }

  def withArchiver[R](
    testWith: TestWith[(Bucket, Bucket, QueuePair, Topic, ArchiverApp), R]) = {
    withLocalSqsQueueAndDlqAndTimeout(15)(queuePair => {
      withLocalSnsTopic { snsTopic =>
        withLocalS3Bucket { ingestBucket =>
          withLocalS3Bucket { storageBucket =>
            withApp(storageBucket, queuePair, snsTopic) { archiver =>
              testWith(
                (ingestBucket, storageBucket, queuePair, snsTopic, archiver))
            }
          }
        }
      }
    })
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

  def createZip(files: List[FileEntry]) = {
    val zipFileName = File.createTempFile("archiver-test", ".zip").getName
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
    (zipFile, zipFileName)
  }

  def createBagItZip(bagName: String,
                     dataFileCount: Int = 1,
                     valid: Boolean = true) = {
    // Create data files
    val dataFiles = (1 to dataFileCount).map { _ =>
      val fileName = randomAlphanumeric()
      val filePath = s"$bagName/data/$fileName.txt"
      val fileContents = Random.nextString(256)
      FileEntry(filePath, fileContents)
    }
    // Create object files
    val objectFiles = (1 to dataFileCount).map { _ =>
      val fileName = randomAlphanumeric()
      val filePath = s"$bagName/data/object/$fileName.jp2"
      val fileContents = Random.nextString(256)
      FileEntry(filePath, fileContents)
    }
    // Create data manifest
    val dataManifest = FileEntry(
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
      s"$bagName/tagmanifest-sha256.txt",
      s"""$dataManifestChecksum  manifest-sha256.txt
         |$bagItFileChecksum  bagit.txt
         |$bagInfoFileChecksum  bag-info.txt
       """.stripMargin.trim
    )
    val allFiles =
      dataFiles ++ objectFiles ++ List(
        dataManifest,
        metaManifest,
        bagInfoFile,
        bagItFile)
    createZip(allFiles.toList)
  }
}

case class FileEntry(name: String, contents: String)
