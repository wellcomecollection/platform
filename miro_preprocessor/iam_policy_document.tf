data "aws_iam_policy_document" "s3_read_miro_data" {
  statement {
    actions = [
      "s3:Get*",
      "s3:List*",
    ]

    resources = [
      "${data.terraform_remote_state.platform.bucket_miro_data_arn}/source",
    ]
  }
}

data "aws_iam_policy_document" "s3_write_miro_data" {
  statement {
    actions = [
      "s3:Put*",
    ]

    resources = [
      "${data.terraform_remote_state.platform.bucket_miro_data_arn}/*",
    ]
  }
}

data "aws_iam_policy_document" "s3_read_miro_json" {
  statement {
    actions = [
      "s3:Get*",
      "s3:List*",
    ]

    resources = [
      "${data.terraform_remote_state.platform.bucket_miro_data_arn}/json",
    ]
  }
}
