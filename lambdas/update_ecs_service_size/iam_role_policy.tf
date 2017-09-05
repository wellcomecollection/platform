# Role policies for the Update ECS Service Size Lambda

resource "aws_iam_role_policy" "update_ecs_service_size_policy" {
  name   = "lambda_update_ecs_service_size"
  role   = "${module.lambda_update_ecs_service_size.role_name}"
  policy = "${data.aws_iam_policy_document.update_ecs_service_size.json}"
}
