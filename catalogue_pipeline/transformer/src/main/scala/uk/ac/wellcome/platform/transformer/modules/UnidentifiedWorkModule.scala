package uk.ac.wellcome.platform.transformer.modules

import com.google.inject.Provides
import com.twitter.inject.{Injector, TwitterModule}
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.semiauto.{deriveDecoder, deriveEncoder}
import javax.inject.Singleton
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.utils.JsonUtil._

import uk.ac.wellcome.platform.transformer.GlobalExecutionContext.context


object UnidentifiedWorkModule extends TwitterModule {
  @Provides
  @Singleton
  def provideUnidentifiedWorkDecoder(): Decoder[UnidentifiedWork] =
    deriveDecoder[UnidentifiedWork]

  @Provides
  @Singleton
  def provideUnidentifiedWorkEncoder(): Encoder[UnidentifiedWork] =
    deriveEncoder[UnidentifiedWork]

  @Provides
  @Singleton
  def provideUnidentifiedWorkStore(injector: Injector): ObjectStore[UnidentifiedWork] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicitly[ObjectStore[UnidentifiedWork]]
  }
}
