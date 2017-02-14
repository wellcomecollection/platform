resource "aws_cloudwatch_log_group" "ecs" {
  name = "platform/ecs-agent"
}

resource "aws_cloudwatch_log_group" "ecs_tools" {
  name = "platform/ecs-agent-tools"
}

resource "aws_cloudwatch_log_group" "app" {
  name = "platform/app"
}

resource "aws_cloudwatch_log_group" "app_jenkins" {
  name = "platform/app-jenkins"
}
