package uk.ac.wellcome.platform.archive.common.models

case class MalformedBagDigestException(line: String, bagName: BagPath)
  extends RuntimeException(
    s"Malformed bag digest line: $line in ${bagName.value}"
  )
