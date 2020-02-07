data "aws_iam_policy_document" "go_hog_wild" {
  statement {
    effect = "Allow"

    actions = [
      "*",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "restrict_destructive_actions" {
  statement {
    effect = "Deny"

    actions = [
      "s3:DeleteBucket*",
      "s3:PutBucket*",
      "s3:PutEncryptionConfiguration",
      "s3:PutInventoryConfiguration",
      "s3:PutLifecycleConfiguration",
      "s3:PutMetricsConfiguration",
      "s3:PutReplicationConfiguration",
      "dynamodb:DeleteTable",
      "rds:DeleteDB*",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "iam_user_group_deny" {
  statement {
    effect = "Allow"

    actions = [
      "iam:*",
    ]

    resources = [
      "*",
    ]
  }

  statement {
    effect = "Deny"

    actions = [
      "iam:CreateUser*",
      "iam:DeleteUser*",
      "iam:AttachUser*",
      "iam:PutUser*",
      "iam:AddUser*",
      "iam:RemoveUser*",
      "iam:TagUser*",
      "iam:UntagUser*",
      "iam:CreateGroup*",
      "iam:DeleteGroup*",
      "iam:PutGroup*",
      "iam:UpdateGroup*",
      "iam:DetachGroup*",
    ]

    resources = [
      "*",
    ]
  }
}
