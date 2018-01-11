resource "aws_s3_bucket" "sierra_data" {
  bucket = "wellcomecollection-sierra-adapter-data"

  lifecycle {
    prevent_destroy = true
  }
}
