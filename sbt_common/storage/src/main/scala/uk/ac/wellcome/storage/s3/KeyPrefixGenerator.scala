package uk.ac.wellcome.storage.s3

import com.google.inject.Inject
import uk.ac.wellcome.models.Sourced

// '-T' means KeyPrefixGenerator is contravariant for type T
//
// This means that if S is a subclass of T, then KeyPrefixGenerator[T]
// is a subclass of KeyPrefixGenerator[S] (the class hierarchy is reversed).
//
// For example,
//
//      Sourced < SierraTransformable
//
// but
//
//      KeyPrefixGenerator[SierraTransformable] < KeyPrefixGenerator[Sourced]
//
// In S3TypedObjectStore, we need a KeyPrefixGenerator[T], where T is usually
// some subclass of Sourced (this is the way we use it, not a strict
// requirement).  This contravariance allows us to define a single
// KeyPrefixGenerator[Sourced] and use it on all instances, even when T is a
// subclass of Sourced.

trait KeyPrefixGenerator[-T] {
  def generate(obj: T): String
}

// To spread objects evenly in our S3 bucket, we take the last two
// characters of the ID and reverse them.  This ensures that:
//
//  1.  It's easy for a person to find the S3 data corresponding to
//      a given source ID.
//
//  2.  Adjacent objects are stored in shards that are far apart,
//      e.g. b0001 and b0002 are separated by nine shards.

class SourcedKeyPrefixGenerator @Inject() extends KeyPrefixGenerator[Sourced] {
  override def generate(obj: Sourced): String = {
    val s3Shard = obj.sourceId.reverse.slice(0, 2)

    s"${obj.sourceName}/${s3Shard}/${obj.sourceId}"
  }
}
