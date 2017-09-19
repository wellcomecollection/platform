module "services_fleet" {
  source            = "../terraform/spot_fleet_request"
  spot_price        = "0.002"
  target_capacity   = 1
  instance_type     = "m4.xlarge"
  image_id          = "${data.aws_ami.stable_coreos.id}"
  key_name          = "${var.key_name}"
  availability_zone = "${var.aws_region}"
  user_data         = "${module.services_userdata.rendered}"
}
