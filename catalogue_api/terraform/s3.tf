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
