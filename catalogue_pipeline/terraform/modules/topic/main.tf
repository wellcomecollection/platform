module "topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v19.7.2"
  name   = "${var.name}"
}

resource "aws_iam_role_policy" "task_sns" {
  count = "${length(var.role_names)}"

  role   = "${var.role_names[count.index]}"
  policy = "${module.topic.publish_policy}"
}

resource "aws_iam_role_policy" "task_s3_messages_put" {
  count = "${length(var.role_names)}"

  role   = "${var.role_names[count.index]}"
  policy = "${data.aws_iam_policy_document.allow_s3_messages_put.json}"
}

data "aws_iam_policy_document" "allow_s3_messages_put" {
  statement {
    actions = [
      "s3:PutObject",
    ]

    resources = [
      "${var.messages_bucket_arn}/*",
    ]
  }
}
