/*
  Lambda for updating ECS service size.

  This is triggered by updates to an SNS topic.
 */

module "lambda_update_ecs_service_size" {
  source      = "./lambda"
  name        = "update_ecs_service_size"
  description = "Update the desired count of an ECS service"
  filename    = "../lambdas/update_ecs_service_size.py"
}

module "update_ecs_service_size_trigger" {
  source               = "./lambda/trigger_sns"
  lambda_function_name = "${module.lambda_update_ecs_service_size.function_name}"
  lambda_function_arn  = "${module.lambda_update_ecs_service_size.arn}"
  sns_trigger_arn      = "${aws_sns_topic.service_scheduler_topic.arn}"
}

data "aws_iam_policy_document" "update_ecs_service_size" {
  statement {
    actions = [
      "ecs:UpdateService",
    ]

    resources = [
      "*",
    ]
  }
}

resource "aws_iam_role_policy" "update_ecs_service_size_policy" {
  name   = "update_ecs_service_size"
  role   = "${module.lambda_update_ecs_service_size.role_name}"
  policy = "${data.aws_iam_policy_document.update_ecs_service_size.json}"
}
