resource "aws_cloudwatch_log_group" "ecs_agent" {
  name = "platform/ecs-agent-${var.cluster_name}"
}
