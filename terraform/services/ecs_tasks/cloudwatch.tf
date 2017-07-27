resource "aws_cloudwatch_log_group" "task" {
  name = "platform/${var.task_name}"
}

resource "aws_cloudwatch_log_group" "nginx_task" {
  name = "platform/nginx_${var.task_name}"
}