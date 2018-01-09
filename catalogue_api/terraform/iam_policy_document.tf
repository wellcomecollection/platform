data "aws_iam_policy_document" "miro_images_sync" {
  statement {
    # Easier than listing the _many_ permissions required for s3 sync to work
    actions = [
      "s3:*",
    ]

    resources = [
      "${aws_s3_bucket.miro-images-sync.arn}/*",
      "${aws_s3_bucket.miro-images-sync.arn}",
    ]
  }

  statement {
    actions = [
      "s3:ListAllMyBuckets",
    ]

    resources = [
      "arn:aws:s3:::*",
    ]
  }
}
