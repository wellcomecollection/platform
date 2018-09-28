package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.ByteArrayInputStream

import akka.stream.ActorAttributes.SupervisionStrategy
import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model._
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.ObjectLocation

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object S3UploadFlow extends Logging {
  def apply(uploadLocation: ObjectLocation)(implicit s3Client: AmazonS3) =
    new S3UploadFlow(uploadLocation)(s3Client)
}

class S3UploadFlow(uploadLocation: ObjectLocation)(implicit s3Client: AmazonS3)
    extends GraphStage[
      FlowShape[ByteString, Try[CompleteMultipartUploadResult]]]
    with Logging {

  val in = Inlet[ByteString]("S3UploadFlow.in")
  val out = Outlet[Try[CompleteMultipartUploadResult]]("S3UploadFlow.out")

  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      private var maybeUploadId: Option[String] = None
      private var partNumber = 1
      private var partEtagList: List[PartETag] = Nil
      private var currentPart: ByteString = ByteString.empty

      override def preStart(): Unit = {
        maybeUploadId = None
        partNumber = 1
        partEtagList = Nil
      }

      val minSize: Int = 5 * 1024 * 1024
      val maxSize: Int = 10 * 1024 * 1024

      setHandler(out, new OutHandler {
        override def onPull(): Unit = pull(in)
      })

      setHandler(
        in,
        new InHandler {
          override def onPush(): Unit = {
            val byteString = grab(in)
            debug(s"Received bytestring of size ${byteString.size}")
            currentPart = currentPart ++ byteString
            uploadIfAboveMinSize(currentPart)
          }

          override def onUpstreamFinish(): Unit = {
            uploadByteString(currentPart)
            maybeUploadId.foreach { uploadId =>
              completeUpload(uploadId)
            }

            completeStage()
          }

          override def onUpstreamFailure(ex: Throwable): Unit = {
            handleFailure(ex)
          }
        }
      )

      private def uploadIfAboveMinSize(byteString: ByteString): Unit = {
        byteString.size match {
          case size if size >= minSize && size < maxSize =>
            debug(s"Current stored bytestring is $maxSize < ${byteString.size} >= $minSize: uploading")
            uploadByteString(byteString)
          case size if size < minSize =>
            debug(s"Current stored bytestring is ${byteString.size} < $minSize: pulling from inlet")
            currentPart = byteString
            pull(in)
          case _ =>
            val (current,next) = byteString.splitAt(maxSize)
            uploadByteString(current)
            if(next.nonEmpty) uploadIfAboveMinSize(next)
        }

      }

      private def uploadByteString(byteString: ByteString): Unit = {
        if(byteString.nonEmpty) {
          debug(s"Uploading bytestring of size ${byteString.size}")
          val triedUploadResult = getUploadId.flatMap { uploadId =>
            val inputStream = new ByteArrayInputStream(byteString.toArray)
            Try(
              s3Client.uploadPart(
                new UploadPartRequest()
                  .withBucketName(uploadLocation.namespace)
                  .withKey(uploadLocation.key)
                  .withUploadId(uploadId)
                  .withInputStream(inputStream)
                  .withPartNumber(partNumber)
                  .withPartSize(byteString.size)
              ))
          }
          triedUploadResult match {
            case Failure(ex) =>
              handleInternalFailure(ex)
            case Success(uploadResult) =>
              partNumber = partNumber + 1
              partEtagList = partEtagList :+ uploadResult.getPartETag
          }
        }
      }

      private def completeUpload(uploadId: String): Unit = {
        debug("Completing upload")
        val compRequest =
          new CompleteMultipartUploadRequest(
            uploadLocation.namespace,
            uploadLocation.key,
            uploadId,
            partEtagList.asJava)
        val res = Try(s3Client.completeMultipartUpload(compRequest))
        res match {
          case Success(result) => push(out, Try(result))
          case Failure(ex)     => handleInternalFailure(ex)
        }
      }

      private def handleInternalFailure(ex: Throwable): Unit = {
        push(out, Failure(ex))
        handleFailure(ex)
      }

      private def handleFailure(ex: Throwable): Unit = {
        error("There was a failure uploading to s3!", ex)
        abortUpload()
        val supervisionStrategy = inheritedAttributes.get[SupervisionStrategy](
          SupervisionStrategy(_ => Supervision.Stop))
        supervisionStrategy.decider(ex) match {
          case Supervision.Stop    => failStage(ex)
          case Supervision.Resume  => completeStage()
          case Supervision.Restart => completeStage()
        }
      }

      private def abortUpload(): Unit = {
        debug("aborting upload")
        maybeUploadId.foreach { uploadId =>
          Try(
            s3Client.abortMultipartUpload(
              new AbortMultipartUploadRequest(
                uploadLocation.namespace,
                uploadLocation.key,
                uploadId)))
        }
      }

      private def getUploadId: Try[String] = maybeUploadId match {
        case None =>
          val triedUploadId = initializeUpload(uploadLocation)
          triedUploadId.foreach(uploadId => maybeUploadId = Some(uploadId))
          triedUploadId
        case Some(initializedId) => Try(initializedId)

      }
      private def initializeUpload(uploadLocation: ObjectLocation) =
        Try(
          s3Client
            .initiateMultipartUpload(
              new InitiateMultipartUploadRequest(
                uploadLocation.namespace,
                uploadLocation.key))
            .getUploadId)
    }

}
