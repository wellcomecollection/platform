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

resource "aws_launch_configuration" "platform" {
  security_groups = [
    "${aws_security_group.instance_sg.id}",
  ]

  key_name                    = "${var.key_name}"
  image_id                    = "${data.aws_ami.stable_coreos.id}"
  instance_type               = "${var.instance_type}"
  iam_instance_profile        = "${module.ecs_main_iam.instance_profile_name}"
  user_data                   = "${data.template_file.platform_userdata.rendered}"
  associate_public_ip_address = true

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_launch_configuration" "tools" {
  security_groups = [
    "${aws_security_group.tools_instance_sg.id}",
  ]

  key_name                    = "${var.key_name}"
  image_id                    = "${data.aws_ami.stable_coreos.id}"
  instance_type               = "${var.instance_type_tools_cluster}"
  iam_instance_profile        = "${aws_iam_instance_profile.app.name}"
  iam_instance_profile        = "${module.ecs_tools_iam.instance_profile_name}"
  user_data                   = "${data.template_file.tools_userdata.rendered}"
  associate_public_ip_address = true

  lifecycle {
    create_before_destroy = true
  }
}
