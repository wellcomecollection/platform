resource "aws_s3_bucket" "miro-data" {
  bucket = "miro-data"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket" "miro-images-sync" {
  bucket = "miro-images-sync"
  acl    = "private"

  lifecycle_rule {
    id      = "move_to_infrequent_access"
    enabled = true
    prefix  = ""

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }
  }
}

# This bucket needs to be removed when we transition to using wellcomecollection-miro-images-public
resource "aws_s3_bucket" "miro_images_public" {
  bucket = "miro-images-public"
  acl    = "public-read"
}

resource "aws_s3_bucket" "cloudfront-logs" {
  bucket = "wellcome-platform-cloudfront-logs"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }
}
