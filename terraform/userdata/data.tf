data "template_file" "template" {
  template = "${file("${path.module}/templates/${local.template_name}.yml.template")}"

  vars {
    aws_region          = "${var.aws_region}"
    ecs_cluster_name    = "${var.cluster_name}"
    ecs_log_level       = "info"
    ecs_agent_version   = "latest"
    ecs_log_group_name  = "${aws_cloudwatch_log_group.ecs_agent.name}"
    efs_filesystem_id   = "${var.efs_filesystem_id}"
    efs_mount_directory = "/mnt/efs"
    ebs_block_device   = "${var.ebs_block_device}"
  }
}

locals {
  template_name = "ecs-agent${var.efs_filesystem_id == "no_name_set" ? "${var.ebs_block_device == "no_name_set" ? "" : "-with-ebs"}" : "-with-efs"}"
}
