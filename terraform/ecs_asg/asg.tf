resource "aws_cloudformation_stack" "ecs_asg" {
  name          = "${var.asg_name}"
  template_body = "${data.template_file.cluster_asg.rendered}"
  depends_on    = ["aws_launch_configuration.launch_config"]

  lifecycle {
    create_before_destroy = true
  }
}

data "template_file" "cluster_asg" {
  template = "${file("${path.module}/asg.json.template")}"

  vars {
    launch_config_name   = "${aws_launch_configuration.launch_config.name}"
    vpc_zone_identifier  = "${jsonencode(var.subnet_list)}"
    asg_min_size         = "${var.asg_min}"
    asg_desired_size     = "${var.asg_desired}"
    asg_max_size         = "${var.asg_max}"
    sns_topic_arn        = "${var.sns_topic_arn}"
    sns_publish_role_arn = "${aws_iam_role.sns_publish_role.arn}"
  }
}

resource "aws_launch_configuration" "launch_config" {
  security_groups = [
    "${aws_security_group.instance_sg.id}",
  ]

  key_name                    = "${var.key_name}"
  image_id                    = "${var.ami_id}"
  instance_type               = "${var.instance_type}"
  iam_instance_profile        = "${var.instance_profile_name}"
  user_data                   = "${var.user_data}"
  associate_public_ip_address = "${var.public_ip}"

  lifecycle {
    create_before_destroy = true
  }
}
