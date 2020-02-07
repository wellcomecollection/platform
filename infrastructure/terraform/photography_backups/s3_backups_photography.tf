resource "aws_s3_bucket" "photography_backups" {
  bucket = "wellcomecollection-backups-photography"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }
}
