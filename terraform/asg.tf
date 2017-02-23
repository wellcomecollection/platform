/* Autoscaling groups described as separate Cloudformation stacks
   in order that rolling updates are possible. */

resource "aws_cloudformation_stack" "tools_cluster_asg" {
  name = "tools-cluster-asg"
  template_body = "${data.template_file.tools_cluster_asg.rendered}"
}

data "template_file" "tools_cluster_asg" {
  template = "${file("${path.module}/cloudformation/asg.json.template")}"

  vars {
    launch_config_name = "${aws_launch_configuration.tools.name}"
    vpc_zone_identifier = "${jsonencode(aws_subnet.tools.*.id)}"
    asg_min_size = "${var.asg_min}"
    asg_desired_size = "${var.asg_desired}"
    asg_max_size = "${var.asg_max}"
  }
}

resource "aws_cloudformation_stack" "platform_cluster_asg" {
  name = "platform-cluster-asg"
  template_body = "${data.template_file.platform_cluster_asg.rendered}"
}

data "template_file" "platform_cluster_asg" {
  template = "${file("${path.module}/cloudformation/asg.json.template")}"

  vars {
    launch_config_name = "${aws_launch_configuration.platform.name}"
    vpc_zone_identifier = "${jsonencode(aws_subnet.main.*.id)}"
    asg_min_size = "${var.asg_min}"
    asg_desired_size = "${var.asg_desired}"
    asg_max_size = "${var.asg_max}"
  }
}
