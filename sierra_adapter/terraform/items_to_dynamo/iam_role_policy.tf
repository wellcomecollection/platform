resource "aws_iam_role_policy" "allow_dynamo_access" {
  role   = "${module.sierra_to_dynamo_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.sierra_table_permissions.json}"
}

resource "aws_iam_role_policy" "allow_read_from_windows_q" {
  role   = "${module.sierra_to_dynamo_service.task_role_name}"
  policy = "${module.demultiplexer_queue.read_policy}"
}