resource "aws_spot_fleet_request" "request" {
  iam_fleet_role  = "${aws_iam_role.fleet_role.arn}"
  spot_price      = "${var.spot_price}"
  target_capacity = "${var.target_capacity}"
  valid_until     = "${var.valid_until}"

  launch_specification {
    instance_type        = "${var.instance_type}"
    ami                  = "${var.image_id}"
    key_name             = "${var.key_name}"
    availability_zone    = "${var.availability_zone}"
    user_data            = "${var.user_data}"
    iam_instance_profile = "${var.instance_profile_name}"
  }
}
