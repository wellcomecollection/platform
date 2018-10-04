module "cloudformation_stack" {
  source = "git::https://github.com/wellcometrust/terraform.git//ec2/modules/asg?ref=v11.7.2"

  asg_name = "${var.name}"

  asg_min     = "0"
  asg_desired = "${var.enabled ? 1 : 0}"
  asg_max     = "1"

  subnet_list        = "${var.subnet_list}"
  launch_config_name = "${module.launch_config.name}"
}

module "launch_config" {
  source = "git::https://github.com/wellcometrust/terraform.git//ec2/modules/launch_config/spot?ref=v11.7.2"

  key_name              = "${var.key_name}"
  image_id              = "${var.image_id}"
  instance_type         = "${var.instance_type}"
  instance_profile_name = "${module.instance_profile.name}"
  user_data             = "${data.template_file.userdata.rendered}"

  associate_public_ip_address = true
  instance_security_groups    = ["${module.security_groups.instance_security_groups}"]

  spot_price = "${var.spot_price}"
}

module "security_groups" {
  source = "git::https://github.com/wellcometrust/terraform.git//ec2/modules/security_groups?ref=v11.7.2"

  name   = "${var.name}"
  vpc_id = "${var.vpc_id}"

  custom_security_groups         = ["${var.custom_security_groups}"]
  controlled_access_cidr_ingress = ["${var.controlled_access_cidr_ingress}"]
}

module "instance_profile" {
  source = "git::https://github.com/wellcometrust/terraform.git//ec2/modules/instance_profile?ref=v11.7.2"

  name = "${var.name}"
}

data "template_file" "requirements" {
  template = "${file("${path.module}/templates/requirements.txt")}"
}

data "template_file" "jupyter_config" {
  template = "${file("${path.module}/templates/jupyter_notebook_config.py.tpl")}"

  vars {
    notebook_user   = "jupyter"
    notebook_port   = "8888"
    hashed_password = "${var.hashed_password}"
    bucket_name     = "${var.bucket_name}"
  }
}

data "template_file" "userdata" {
  template = "${file("${path.module}/templates/userdata.sh.tpl")}"

  vars {
    jupyter_notebook_config = "${data.template_file.jupyter_config.rendered}"
    requirements            = "${data.template_file.requirements.rendered}"
    notebook_user           = "jupyter"
    default_environment     = "${var.default_environment}"
    efs_mount_id            = "${var.efs_mount_id}"
  }
}

resource "aws_autoscaling_schedule" "scale_down" {
  scheduled_action_name  = "ensure_down"
  min_size               = 0
  max_size               = 1
  desired_capacity       = 0
  recurrence             = "0 20 * * *"
  autoscaling_group_name = "${module.cloudformation_stack.asg_name}"
}
