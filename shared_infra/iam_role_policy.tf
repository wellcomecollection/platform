resource "aws_iam_role_policy" "sqs_freezeray_sqs_access" {
  role   = "${module.ecs_sqs_freezeray_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.sqs_readwrite.json}"
}

resource "aws_iam_role_policy" "sqs_freezeray_s3_access" {
  role   = "${module.ecs_sqs_freezeray_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.upload_sqs_freeze.json}"
}

resource "aws_iam_role_policy" "sqs_redrive_sqs_read_delete_send" {
  role   = "${module.ecs_sqs_redrive_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.sqs_readwritesend.json}"
}
