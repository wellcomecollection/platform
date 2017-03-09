data "template_file" "template" {
  template = "${file("${path.module}/templates/${var.template_name}.yml.template")}"

  vars {
    aws_region         = "${var.aws_region}"
    ecs_cluster_name   = "${var.cluster_name}"
    ecs_log_level      = "info"
    ecs_agent_version  = "latest"
    ecs_log_group_name = "${aws_cloudwatch_log_group.ecs_agent.name}"
    efs_fs_name        = "${var.efs_fs_name}"
  }
}

resource "aws_cloudwatch_log_group" "ecs_agent" {
  name = "platform/ecs-agent-${var.cluster_name}"
}
