resource "aws_s3_bucket" "vhs_bucket" {
  bucket = "wellcomecollection-platform-recorder-vhs-bucket"

  lifecycle {
    prevent_destroy = true
  }
}
