resource "aws_cloudformation_stack" "ecs_asg" {
  name          = "${var.asg_name}"
  template_body = "${data.template_file.cluster_asg.rendered}"
}

data "template_file" "cluster_asg" {
  template = "${file("${path.module}/asg.json.template")}"

  vars {
    launch_config_name  = "${aws_launch_configuration.launch_config.name}"
    vpc_zone_identifier = "${jsonencode(var.subnet_list)}"
    asg_min_size        = "${var.asg_min}"
    asg_desired_size    = "${var.asg_desired}"
    asg_max_size        = "${var.asg_max}"
  }
}

resource "aws_launch_configuration" "launch_config" {
  security_groups = [
    "${aws_security_group.instance_sg.id}",
  ]

  key_name                    = "${var.key_name}"
  image_id                    = "${data.aws_ami.stable_coreos.id}"
  instance_type               = "${var.instance_type}"
  iam_instance_profile        = "${var.instance_profile_name}"
  user_data                   = "${var.user_data}"
  associate_public_ip_address = "${var.public_ip}"

  lifecycle {
    create_before_destroy = true
  }
}
