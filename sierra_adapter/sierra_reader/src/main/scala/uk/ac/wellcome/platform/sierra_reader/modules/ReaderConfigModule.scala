package uk.ac.wellcome.platform.sierra_reader.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.platform.sierra_reader.models.ReaderConfig

object ReaderConfigModule extends TwitterModule {
  private val batchSize = flag[Int](
    "reader.batchSize",
    50,
    "Number of records in a single json batch")

  @Singleton
  @Provides
  def providesReaderConfig(): ReaderConfig =
    ReaderConfig(
      batchSize = batchSize()
    )
}
