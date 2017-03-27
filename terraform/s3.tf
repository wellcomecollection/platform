resource "aws_s3_bucket" "miro-data" {
  bucket = "miro-data"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }
}
