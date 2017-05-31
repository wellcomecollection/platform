package uk.ac.wellcome.test.utils


trait TestFlags {
  val testFlags = Map(
    // use a fake endpoint in tests so that we don't send metrics to the real AWS
    "aws.cloudWatch.endpoint" -> "http://localhost:6789"
  )
}
