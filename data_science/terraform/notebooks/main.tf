module "p2_compute" {
  source = "git::https://github.com/wellcometrust/terraform.git//ec2/prebuilt/dlami?ref=v11.7.1"
  name   = "jupyter-p2-${var.namespace}"

  key_name    = "${var.key_name}"
  bucket_name = "${var.s3_bucket_name}"

  instance_type = "p2.xlarge"
  spot_price    = "0.5"

  default_environment = "tensorflow_p36"

  vpc_id      = "${var.vpc_id}"
  subnet_list = "${var.subnets}"

  custom_security_groups = [
    "${aws_security_group.efs_security_group.id}",
  ]

  efs_mount_id = "${module.efs.efs_id}"

  controlled_access_cidr_ingress = ["${var.controlled_access_cidr_ingress}"]
}

module "t2_compute" {
  source = "git::https://github.com/wellcometrust/terraform.git//ec2/prebuilt/dlami?ref=v11.7.1"
  name   = "jupyter-t2-${var.namespace}"

  key_name    = "${var.key_name}"
  bucket_name = "${var.s3_bucket_name}"

  spot_price = "0.4"

  default_environment = "tensorflow_p36"

  vpc_id      = "${var.vpc_id}"
  subnet_list = "${var.subnets}"

  custom_security_groups = [
    "${aws_security_group.efs_security_group.id}",
  ]

  efs_mount_id = "${module.efs.efs_id}"

  controlled_access_cidr_ingress = ["${var.controlled_access_cidr_ingress}"]
}
