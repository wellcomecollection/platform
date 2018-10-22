resource "aws_iam_role_policy" "ecs_transformer_task_vhs" {
  role   = "${module.lambda_transformer_example.role_name}"
  policy = "${local.miro_vhs_read_policy}"
}

resource "aws_iam_role_policy" "ecs_transformer_task_vhs" {
  role   = "${module.lambda_transformer_example.role_name}"
  policy = "${local.sierra_vhs_read_policy}"
}
