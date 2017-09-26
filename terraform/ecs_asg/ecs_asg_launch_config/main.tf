resource "aws_launch_configuration" "ondemand_launch_config" {
  count = "${1 - var.use_spot}"

  security_groups = [
    "${aws_security_group.instance_sg.id}",
  ]

  key_name                    = "${var.key_name}"
  image_id                    = "${var.image_id}"
  instance_type               = "${var.instance_type}"
  iam_instance_profile        = "${var.instance_profile_name}"
  user_data                   = "${var.user_data}"
  associate_public_ip_address = "${var.public_ip}"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_launch_configuration" "spot_launch_config" {
  count      = "${var.use_spot}"
  spot_price = "${var.spot_price}"

  security_groups = [
    "${aws_security_group.instance_sg.id}",
  ]

  key_name                    = "${var.key_name}"
  image_id                    = "${var.image_id}"
  instance_type               = "${var.instance_type}"
  iam_instance_profile        = "${var.instance_profile_name}"
  user_data                   = "${var.user_data}"
  associate_public_ip_address = "${var.public_ip}"

  lifecycle {
    create_before_destroy = true
  }
}
