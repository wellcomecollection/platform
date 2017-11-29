resource "aws_iam_role_policy" "dynamo_to_miro_sns" {
  role   = "${module.lambda_dynamo_to_sns.role_name}"
  policy = "${data.aws_iam_role_policy.publish_to_topic.json}"
}

data "aws_iam_policy_document" "publish_to_topic" {
  statement {
    actions = [
      "sns:Publish",
    ]

    resources = [
      "${var.dst_topic_arn}",
    ]
  }
}
