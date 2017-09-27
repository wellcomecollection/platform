data "aws_iam_policy_document" "wellcomecollection-miro-images-public" {
  statement {
    actions = [
      "s3:GetObject",
    ]

    resources = [
      "arn:aws:s3:::${aws_s3_bucket.wellcomecollection-miro-images-public.id}/*",
    ]

    principals {
      type        = "AWS"
      identifiers = ["*"]
    }
  }
}