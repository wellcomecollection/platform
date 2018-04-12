resource "aws_s3_bucket_policy" "wellcomecollection-miro-images-public" {
  bucket = "${aws_s3_bucket.wellcomecollection-miro-images-public.id}"
  policy = "${data.aws_iam_policy_document.wellcomecollection-miro-images-public.json}"
}

resource "aws_s3_bucket" "wellcomecollection-miro-images-public" {
  bucket = "wellcomecollection-miro-images-public"
  acl    = "public-read"

  lifecycle {
    prevent_destroy = true
  }
}
