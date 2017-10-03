resource "aws_iam_user" "client" {
  name = "${var.username}"
}

resource "aws_iam_access_key" "client" {
  user    = "${aws_iam_user.client.name}"
  pgp_key = "keybase:${var.username}"
}

resource "aws_iam_user_policy" "client_s3_client_transfer_bucket_listbucket_r3store" {
  user   = "${aws_iam_user.client.name}"
  policy = "${data.aws_iam_policy_document.s3_client_transfer_bucket_listbucket.json}"
}

resource "aws_iam_user_policy" "client_s3_client_transfer_bucket_all_s3_actions_r3store" {
  user   = "${aws_iam_user.client.name}"
  policy = "${data.aws_iam_policy_document.s3_client_transfer_bucket_all_s3_actions.json}"
}

data "aws_iam_policy_document" "s3_client_transfer_bucket_listbucket" {
  statement {
    actions = [
      "s3:ListBucket",
    ]

    resources = [
      "${var.bucket_arn}",
    ]

    condition {
      test     = "StringLike"
      variable = "s3:prefix"

      values = ["${var.user_path}/*"]
    }
  }
}

data "aws_iam_policy_document" "s3_client_transfer_bucket_all_s3_actions" {
  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "${var.bucket_arn}/${var.user_path}/*",
    ]
  }
}
