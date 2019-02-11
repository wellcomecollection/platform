resource "aws_s3_bucket" "releases" {
  bucket = "releases.mvn-repo.wellcomecollection.org"
  acl    = "public-read"

  lifecycle {
    prevent_destroy = true
  }

  lifecycle_rule {
    id = "transition_all_to_standard_ia"

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    enabled = true
  }
}
