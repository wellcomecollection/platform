resource "aws_s3_bucket" "jupyter" {
  bucket = "wellcomecollection-platform-jupyter"
  acl    = "private"

  lifecycle_rule {
    id      = "tmp"
    prefix  = "tmp/"
    enabled = true

    expiration {
      days = 30
    }
  }

  lifecycle {
    prevent_destroy = true
  }
}
