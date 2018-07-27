package uk.ac.wellcome.platform.archiver

import java.io.InputStream
import java.util.zip.ZipFile

import uk.ac.wellcome.storage.ObjectLocation


trait HasBagDigest[T] {
  def getDigest(t: T, location: ObjectLocation): Either[MalformedBagDigestError, List[BagDigestItem]]
}

object HasBagDigest {
  val bagItDigestDelimiter = "  "

  def apply[T](implicit d: HasBagDigest[T]): HasBagDigest[T] = d

  implicit class HasBagDigestOps[T: HasBagDigest](t: T) {
    def getDigest(location: ObjectLocation) = HasBagDigest[T].getDigest(t, location)
  }

  private def readFile(inputStream: InputStream) = {
    val lines = scala.io.Source.fromInputStream(inputStream).getLines
    lines.map(_.split(bagItDigestDelimiter).map(_.trim))
  }

  implicit val zipHasBagDigest: HasBagDigest[ZipFile] =
    new HasBagDigest[ZipFile] {
      def getDigest(zipFile: ZipFile, location: ObjectLocation):
        Either[MalformedBagDigestError, List[BagDigestItem]] = {

        val entry = zipFile.getEntry(s"${location.namespace}/${location.key}")
        val inputStream = zipFile.getInputStream(entry)
        val lines = readFile(inputStream)

        val parsedLines = lines.zipWithIndex.map {
          case (Array(checksum: String, key: String), i) => Right(
            BagDigestItem(checksum, ObjectLocation(location.namespace, key))
          )
          case (default, i) => Left(
            MalformedBagDigestLine(default.mkString(bagItDigestDelimiter), i)
          )
        }.toList

        val malformedBagDigestErrors = parsedLines.collect {
          case Left(malformedBagDigestLine) => malformedBagDigestLine
        }

        val bagDigestItems = parsedLines.collect {
          case Right(item) => item
        }

        if(malformedBagDigestErrors.isEmpty) {
          Right(bagDigestItems)
        } else {
          Left(MalformedBagDigestError(malformedBagDigestErrors, location))
        }
      }
    }
}

case class BagDigestItem(checksum: String, location: ObjectLocation)
case class MalformedBagDigestLine(line: String, index: Int)
case class MalformedBagDigestError(errors: List[MalformedBagDigestLine], bagLocation: ObjectLocation) {
  override def toString: String = {
    errors.map {
      case MalformedBagDigestLine(line, index) =>
        s"Malformed digest line at /${bagLocation.namespace}/${bagLocation.key} L$index: $line"
    }.mkString("\n")
  }
}