module "compute" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//ec2/asg?ref=v9.3.0"
  name   = "${var.name}"

  image_id = "${var.ami_id}"
  key_name = "${var.key_name}"

  subnet_list = ["${var.vpc_subnets}"]
  vpc_id      = "${var.vpc_id}"

  use_spot   = 1
  spot_price = "0.4"

  asg_min     = "${var.enabled ? 1 : 0}"
  asg_desired = "1"
  asg_max     = "1"

  instance_type = "${var.instance_type}"

  user_data = "${data.template_file.userdata.rendered}"
}

data "template_file" "userdata" {
  template = "${file("userdata.sh.tpl")}"

  vars {
    notebook_user   = "jupyter"
    notebook_port   = "8888"
    hashed_password = "${var.hashed_password}"
    bucket_name     = "${var.bucket_name}"
  }
}

resource "aws_autoscaling_schedule" "scale_down" {
  scheduled_action_name  = "ensure_down"
  min_size               = 0
  max_size               = 1
  desired_capacity       = 0
  recurrence             = "0 20 * * *"
  autoscaling_group_name = "${module.compute.asg_name}"
}
