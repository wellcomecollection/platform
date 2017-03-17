resource "aws_cloudformation_stack" "ecs_asg" {
  name          = "${var.asg_name}"
  template_body = "${data.template_file.cluster_asg.rendered}"
}

data "template_file" "cluster_asg" {
  template = "${file("${path.module}/asg.json.template")}"

  vars {
    launch_config_name  = "${var.launch_config_name}"
    vpc_zone_identifier = "${jsonencode(var.subnet_list)}"
    asg_min_size        = "${var.asg_min}"
    asg_desired_size    = "${var.asg_desired}"
    asg_max_size        = "${var.asg_max}"
  }
}
