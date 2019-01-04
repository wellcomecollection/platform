data "aws_iam_policy_document" "enable_kms_iam" {
  statement {
    sid    = "Enable IAM User Permissions"
    effect = "Allow"

    principals = {
      type        = "AWS"
      identifiers = ["arn:aws:iam::${var.account_id}:root"]
    }

    actions = [
      "kms:*",
    ]

    resources = ["*"]
  }
}

data "aws_iam_policy_document" "use_encryption_key" {
  statement {
    sid    = "AllowUseOfTheKey"
    effect = "Allow"

    actions = [
      "kms:Encrypt",
      "kms:ReEncrypt*",
      "kms:Decrypt",
      "kms:DescribeKey",
      "kms:GenerateDataKey*",
    ]

    resources = ["${aws_kms_key.encryption_key.arn}"]
  }
}
