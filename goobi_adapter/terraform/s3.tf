resource "aws_s3_bucket" "goobi_adapter" {
  bucket = "wellcomecollection-platform-adapters-goobi"

  lifecycle {
    prevent_destroy = true
  }
}
