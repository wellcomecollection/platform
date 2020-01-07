resource "aws_s3_bucket" "elasticsearch-snapshots-catalogue" {
  bucket = "wellcomecollection-elasticsearch-snapshots-catalogue"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }
}
