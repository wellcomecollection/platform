resource "aws_s3_bucket" "sierra_adapter" {
  bucket = "wellcomecollection-platform-adapters-sierra"

  lifecycle {
    prevent_destroy = true
  }
}
