package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.{ByteArrayInputStream, InputStream}
import java.security.MessageDigest

import akka.NotUsed
import akka.stream.ActorAttributes.SupervisionStrategy
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Source, StreamConverters, Zip}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.model.UploadResult
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.ObjectLocation

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.concurrent.Promise
import scala.util.{Failure, Success, Try}


object S3UploadFlow extends Logging{
  def apply(uploadLocation: ObjectLocation)(implicit s3Client: AmazonS3) = new S3UploadFlow(uploadLocation)(s3Client)
}

class S3UploadFlow(uploadLocation: ObjectLocation)(implicit s3Client: AmazonS3)
  extends GraphStage[FlowShape[ByteString, Try[CompleteMultipartUploadResult]]]
    with Logging {

  val in = Inlet[ByteString]("S3UploadFlow.in")
  val out = Outlet[Try[CompleteMultipartUploadResult]]("S3UploadFlow.out")

  override val shape = FlowShape[ByteString, Try[CompleteMultipartUploadResult]](in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      private var maybeUploadId: Option[String] = None
      private var partNumber = 1
      private var partEtagList: List[PartETag] = Nil
      override def preStart(): Unit = {
        maybeUploadId = None
        partNumber = 1
        partEtagList = Nil
      }

      val maxSize = 5 * 1024 * 1024

      setHandler(out, new OutHandler {
        override def onPull(): Unit = pull(in)
      })

      setHandler(
        in,
        new InHandler {
          override def onPush(): Unit = {
            val byteString = grab(in)
            uploadByteString(byteString)
          }

          override def onUpstreamFinish(): Unit = {
            maybeUploadId match {
              case Some(uploadId) =>
                val compRequest =
                  new CompleteMultipartUploadRequest(
                    uploadLocation.namespace,
                    uploadLocation.key,
                    uploadId,
                    partEtagList.asJava)
                val res = Try(s3Client.completeMultipartUpload(compRequest))

                push(out, res)
              case None => ()
            }

            completeStage()
          }

          override def onUpstreamFailure(ex: Throwable): Unit = {
            abortUpload()
            handleFailure(ex)
          }
        }
      )

      private def handleFailure(ex: Throwable) = {
        val supervisionStrategy = inheritedAttributes.get[SupervisionStrategy](
          SupervisionStrategy(_ => Supervision.Stop))
        supervisionStrategy.decider(ex) match {
          case Supervision.Stop    => failStage(ex)
          case Supervision.Resume  => completeStage()
          case Supervision.Restart => ()
        }
      }
      @tailrec
      private def uploadByteString(byteString: ByteString): Unit = {
        if (byteString.nonEmpty) {
          val (current, next) = byteString.splitAt(maxSize)
          info(s"Uploading chunk ${current.size}")
          val array = current.toArray
          val triedUploadResult = Try(
            s3Client.uploadPart(
              new UploadPartRequest()
                .withBucketName(uploadLocation.namespace)
                .withKey(uploadLocation.key)
                .withUploadId(getUploadId)
                .withInputStream(new ByteArrayInputStream(array))
                .withPartNumber(partNumber)
                .withPartSize(array.length)
            ))
          triedUploadResult match {
            case Failure(ex) =>
              abortUpload()
              handleFailure(ex)
            case Success(uploadResult) =>
              partNumber = partNumber + 1
              partEtagList = partEtagList :+ uploadResult.getPartETag
              info(s"Next chunk of size ${next.size}")
              uploadByteString(next)
          }
        }
      }
      private def abortUpload() = {
        maybeUploadId match {
          case Some(uploadId) =>
            Try(
              s3Client.abortMultipartUpload(
          new AbortMultipartUploadRequest(
            uploadLocation.namespace,
            uploadLocation.key,
            uploadId)))
          case None => ()
        }
      }
      private def getUploadId = {
      maybeUploadId match {
        case None =>
          val initializedId = initializeUpload(uploadLocation)
          maybeUploadId = Some(initializedId)
          initializedId
        case Some(initializedId) => initializedId
      }
    }
      private def initializeUpload(uploadLocation: ObjectLocation)= {
        val initRequest = new InitiateMultipartUploadRequest(uploadLocation.namespace, uploadLocation.key)
        val initResponse = s3Client.initiateMultipartUpload(initRequest)

        initResponse.getUploadId
      }
    }


}
