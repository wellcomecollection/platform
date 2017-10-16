resource "aws_s3_bucket" "wellcomecollection-images" {
  bucket = "wellcomecollection-images"
  acl    = "private"

  lifecycle_rule {
    id     = "library"
    prefix = "library/"

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    enabled = true
  }

  lifecycle {
    prevent_destroy = true
  }
}
