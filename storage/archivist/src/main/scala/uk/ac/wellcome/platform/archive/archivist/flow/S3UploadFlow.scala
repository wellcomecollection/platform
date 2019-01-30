package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.ByteArrayInputStream

import akka.stream.ActorAttributes.SupervisionStrategy
import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model._
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.storage.ObjectMetadata
import uk.ac.wellcome.storage.ObjectLocation

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object S3UploadFlow extends Logging {
  def apply(uploadLocation: ObjectLocation,
            maybeUploadMetadata: Option[ObjectMetadata] = None)(
    implicit s3Client: AmazonS3) =
    new S3UploadFlow(uploadLocation, maybeUploadMetadata)(s3Client)
}

class S3UploadFlow(
  uploadLocation: ObjectLocation,
  maybeUploadMetadata: Option[ObjectMetadata])(implicit s3Client: AmazonS3)
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

      // invoked at startup
      override def preStart(): Unit = {
        maybeUploadId = None
        partNumber = 1
        partEtagList = Nil
      }

      setHandler(out, new OutHandler {
        override def onPull(): Unit = pull(in)

        override def onDownstreamFinish(): Unit = completeStage()
      })

      setHandler(
        in,
        new InHandler {
          override def onPush(): Unit = {
            val byteString = grab(in)
            uploadIfAboveMinSize(
              currentPart ++ byteString,
              isLast = false
            )
            pull(in)
          }

          override def onUpstreamFinish(): Unit = {
            uploadIfAboveMinSize(currentPart, isLast = true)
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

      // The AWS docs tell us that each part of an S3 MultipartUpload can
      // be between 5MB to 5GB, with the last part allowed to be <5MB.
      //
      // We set a smaller maximum size so we don't run out of memory.
      // TODO: The maxSize should be configurable.
      //
      // See https://docs.aws.amazon.com/AmazonS3/latest/dev/qfacts.html
      //
      val minSize: Int = 5 * 1024 * 1024
      val maxSize: Int = 10 * 1024 * 1024

      /** Upload a byte string, taking into account the S3 Upload limits.
        *
        * In particular, it checks the string is large enough to be uploaded,
        * and slices it into smaller pieces if it's too big.
        *
        */
      @tailrec
      private def uploadIfAboveMinSize(byteString: ByteString,
                                       isLast: Boolean): Unit =
        byteString.size match {
          case size if size < minSize && !isLast =>
            currentPart = byteString
          case size if size < maxSize =>
            uploadByteString(byteString)
            currentPart = ByteString.empty
          case _ =>
            val (current, next) = byteString.splitAt(maxSize)
            uploadByteString(current)
            if (next.nonEmpty) uploadIfAboveMinSize(next, isLast)
        }

      /** Upload a single byte string as part of a Multipart Upload.
        *
        * If the upload succeeds, it increments the internal counter and
        * part number so the next part is uploaded after this one.
        *
        * If the upload fails, it aborts the upload and triggers a downstream
        * exception.
        *
        */
      private def uploadByteString(byteString: ByteString): Unit =
        if (byteString.nonEmpty) {
          val triedUploadResult = getUploadId.flatMap { uploadId =>
            val inputStream = new ByteArrayInputStream(byteString.toArray)
            val uploadRequest = new UploadPartRequest()
              .withBucketName(uploadLocation.namespace)
              .withKey(uploadLocation.key)
              .withUploadId(uploadId)
              .withInputStream(inputStream)
              .withPartNumber(partNumber)
              .withPartSize(byteString.size)
            Try(s3Client.uploadPart(uploadRequest))
          }
          triedUploadResult match {
            case Failure(ex) =>
              handleInternalFailure(ex)
            case Success(uploadResult) =>
              partNumber = partNumber + 1
              partEtagList = partEtagList :+ uploadResult.getPartETag
          }
        }

      /** Mark the upload as complete.
        *
        * This tells S3 that we've uploaded every part of the file, and it
        * should assemble them into a single object.
        *
        */
      private def completeUpload(uploadId: String): Unit = {
        debug("Completing upload")
        val compRequest = new CompleteMultipartUploadRequest(
          uploadLocation.namespace,
          uploadLocation.key,
          uploadId,
          partEtagList.asJava
        )
        val res = Try(s3Client.completeMultipartUpload(compRequest))
        res match {
          case Success(result) =>
            debug(s"Upload completed successfully: $uploadLocation")
            push(out, Try(result))
          case Failure(ex) =>
            warn(
              s"Exception while completing upload: $uploadLocation : ${ex.getMessage}")
            handleInternalFailure(ex)
        }
      }

      /** Propagate an internal failure to the person reading from this flow,
        * and trigger a cleanup.
        *
        */
      private def handleInternalFailure(ex: Throwable): Unit = {
        push(out, Failure(ex))
        handleFailure(ex)
      }

      /** Handle a failure in the upload process by aborting the S3 upload,
        * and passing the exception to the Akka supervision handler.
        *
        */
      private def handleFailure(ex: Throwable): Unit = {
        warn(s"There was an exception while uploading to s3 : ${ex.getMessage}")
        abortUpload()
        val supervisionStrategy = inheritedAttributes.get[SupervisionStrategy](
          SupervisionStrategy(_ => Supervision.Stop))
        supervisionStrategy.decider(ex) match {
          case Supervision.Stop    => failStage(ex)
          case Supervision.Resume  => completeStage()
          case Supervision.Restart => completeStage()
        }
      }

      /** Abort the upload -- this prevents any further uploads to this ID,
        * and means that nothing gets written to S3.  Any previously uploaded
        * parts get discarded.
        *
        */
      private def abortUpload(): Unit = {
        debug("aborting upload")
        maybeUploadId.foreach { uploadId =>
          Try(
            s3Client.abortMultipartUpload(
              new AbortMultipartUploadRequest(
                uploadLocation.namespace,
                uploadLocation.key,
                uploadId
              )
            )
          )
        }
      }

      // TODO: How does this work???
      private def getUploadId: Try[String] = maybeUploadId match {
        case None =>
          val triedUploadId =
            initializeUpload(uploadLocation, maybeUploadMetadata)
          triedUploadId.foreach(uploadId => maybeUploadId = Some(uploadId))
          triedUploadId
        case Some(initializedId) => Try(initializedId)

      }

      /** Start a new Multipart Upload with S3, and return the upload ID
        * (if successful).
        *
        */
      private def initializeUpload(
        uploadLocation: ObjectLocation,
        maybeUploadMetadata: Option[ObjectMetadata]): Try[String] = {

        debug(s"initializeUpload: $uploadLocation")

        val initiateRequest = maybeUploadMetadata match {
          case None =>
            new InitiateMultipartUploadRequest(
              uploadLocation.namespace,
              uploadLocation.key
            )
          case Some(objectMetadata) =>
            debug(s"upload with metadata: $objectMetadata")
            new InitiateMultipartUploadRequest(
              uploadLocation.namespace,
              uploadLocation.key,
              objectMetadata.toS3ObjectMetadata
            )
        }
        Try(
          s3Client
            .initiateMultipartUpload(initiateRequest)
            .getUploadId
        )
      }
    }
}
