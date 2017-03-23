/*
  Lambda for updating ECS service size.

  This is triggered by updates to an SNS topic.
 */

module "update_ecs_service_size_lambda" {
  source      = "./lambda"
  name        = "update_ecs_service_size"
  description = "Update the desired count of an ECS service"
  filename    = "../lambdas/update_ecs_service_size.py"
}

module "update_ecs_service_size_trigger" {
  source               = "./lambda/trigger_sns"
  lambda_function_name = "${module.update_ecs_service_size_lambda.function_name}"
  lambda_function_arn  = "${module.update_ecs_service_size_lambda.arn}"
  sns_trigger_arn      = "${aws_sns_topic.service_scheduler_topic.arn}"
}

resource "aws_iam_role_policy" "update_ecs_service_size_policy" {
  name = "update_ecs_service_size"
  role = "${module.update_ecs_service_size_lambda.role_name}"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecs:UpdateService"
      ],
      "Resource": "*"
    }
  ]
}
EOF
}
