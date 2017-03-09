data "template_file" "platform_userdata" {
  template = "${file("${path.module}/userdata/ecs-agent.yml")}"

  vars {
    aws_region         = "${var.aws_region}"
    ecs_cluster_name   = "${aws_ecs_cluster.main.name}"
    ecs_log_level      = "info"
    ecs_agent_version  = "latest"
    ecs_log_group_name = "${aws_cloudwatch_log_group.ecs.name}"
  }
}

data "template_file" "tools_userdata" {
  template = "${file("${path.module}/userdata/ecs-agent-tools.yml")}"

  vars {
    aws_region         = "${var.aws_region}"
    ecs_cluster_name   = "${aws_ecs_cluster.tools.name}"
    ecs_log_level      = "info"
    ecs_agent_version  = "latest"
    ecs_log_group_name = "${aws_cloudwatch_log_group.ecs_tools.name}"
    efs_fs_name        = "${aws_efs_file_system.jenkins.id}"
  }
}
