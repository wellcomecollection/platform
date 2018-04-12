locals {
  public_data_bucket_name = "wellcomecollection-data-public"
}

resource "aws_s3_bucket" "public_data" {
  bucket = "${local.public_data_bucket_name}"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }

  policy = "${data.aws_iam_policy_document.public_data_bucket_get_access_policy.json}"
}

# This file is served from the root of data.wellcomecollection.org.
resource "aws_s3_bucket_object" "index_page" {
  bucket = "${aws_s3_bucket.public_data.id}"
  key    = "index.html"
  source = "data_wc_index.html"
  etag   = "${md5(file("data_wc_index.html"))}"

  content_type = "text/html"
}
