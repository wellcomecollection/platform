module "services_fleet" {
  source          = "../terraform/spot_fleet_request"
  spot_price      = "0.1"
  target_capacity = 2
  instance_type   = "m4.xlarge"

  image_id              = "${data.aws_ami.stable_coreos.id}"
  key_name              = "${var.key_name}"
  availability_zone     = "eu-west-1a"
  user_data             = "${module.services_userdata.rendered}"
  instance_profile_name = "${module.ecs_services_iam.instance_profile_name}"
}
