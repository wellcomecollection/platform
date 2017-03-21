module "ecs_services_iam" {
  source = "./ecs_iam"
  name   = "services"
}

module "ecs_api_iam" {
  source = "./ecs_iam"
  name   = "api"
}

module "ecs_tools_iam" {
  source = "./ecs_iam"
  name   = "tools"
}

resource "aws_iam_role_policy" "ecs_jenkins_task" {
  name = "tf_ecs_task_jenkins_policy"
  role = "${module.ecs_tools_iam.task_role_name}"

  policy = <<EOF
{
  "Version": "2012-10-17",
    "Statement": [
    {
      "Effect": "Allow",
      "Action": "*",
      "Resource": "*"
    }
    ]
}
EOF
}
