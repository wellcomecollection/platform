resource "aws_cloudwatch_log_group" "app" {
  name = "platform/app"
}

resource "aws_cloudwatch_log_group" "app_jenkins" {
  name = "platform/app-jenkins"
}
