module "p2_compute" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//ec2/asg?ref=v9.3.0"
  name   = "jupyter-p2"

  # Ubuntu DLAMI
  image_id = "ami-0bc19972"
  key_name = "${var.key_name}"

  subnet_list = "${module.vpc.subnets}"
  vpc_id      = "${module.vpc.vpc_id}"

  use_spot   = 1
  spot_price = "0.4"

  asg_min     = "0"
  asg_desired = "1"
  asg_max     = "1"

  instance_type = "p2.xlarge"

  user_data = "${data.template_file.userdata.rendered}"
}

module "t2_compute" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//ec2/asg?ref=v9.3.0"
  name   = "jupyter-t2"

  # Ubuntu DLAMI
  image_id = "ami-0bc19972"
  key_name = "${var.key_name}"

  subnet_list = "${module.vpc.subnets}"
  vpc_id      = "${module.vpc.vpc_id}"

  use_spot   = 1
  spot_price = "0.4"

  asg_min     = "0"
  asg_desired = "1"
  asg_max     = "1"

  instance_type = "t2.large"

  user_data = "${data.template_file.userdata.rendered}"
}

# Scale down to 0 every night at 8pm
resource "aws_autoscaling_schedule" "ensure_down" {
  scheduled_action_name  = "ensure_down"
  min_size               = 0
  max_size               = 1
  desired_capacity       = 0
  recurrence             = "0 20 * * *"
  autoscaling_group_name = "${module.t2_compute.asg_name}"
}

resource "aws_autoscaling_schedule" "ensure_down" {
  scheduled_action_name  = "ensure_down"
  min_size               = 0
  max_size               = 1
  desired_capacity       = 0
  recurrence             = "0 20 * * *"
  autoscaling_group_name = "${module.p2_compute.asg_name}"
}

data "template_file" "userdata" {
  template = "${file("userdata.sh.tpl")}"

  vars {
    notebook_user   = "jupyter"
    notebook_port   = "8888"
    hashed_password = "${var.hashed_password}"
    bucket_name     = "${aws_s3_bucket.data_science.id}"
  }
}
