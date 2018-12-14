package uk.ac.wellcome.platform.archive.common.progress.models
import com.gu.scanamo.DynamoFormat
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.storage.ObjectLocation

case class StorageLocation(provider: StorageProvider, location: ObjectLocation)

sealed trait StorageProvider{val id: String}

case object StandardStorageProvider extends  StorageProvider{
  override val id: String = "aws-s3-standard"
}
case object InfrequentAccessStorageProvider extends  StorageProvider{
  override val id: String = "aws-s3-ia"
}

object StorageProvider {
 implicit val dynamoFormat = DynamoFormat.coercedXmap[StorageProvider, String, Throwable](read = fromJson[StorageProvider](_).get )(write = toJson(_).get)
}