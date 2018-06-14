package uk.ac.wellcome.platform.goobi_reader.models

import java.time.Instant

case class S3Event(Records: List[S3Record])
case class S3Record(eventTime: Instant, s3: S3Location)
case class S3Location(bucket: S3Bucket, `object`: S3Object)
case class S3Bucket(name: String)
case class S3Object(key: String)
