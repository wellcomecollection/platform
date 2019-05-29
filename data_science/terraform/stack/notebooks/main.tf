module "p2_compute" {
  source = "../../modules/dlami"
  name   = "jupyter-p2-${var.namespace}"

  key_name    = "${var.key_name}"
  bucket_name = "${var.s3_bucket_name}"

  instance_type = "p3.2xlarge"
  spot_price    = "1.4"

  default_environment = "pytorch_p36"

  vpc_id      = "${var.vpc_id}"
  subnet_list = "${var.subnets}"

  custom_security_groups = [
    "${var.efs_security_group_id}",
  ]

  efs_mount_id = "${var.efs_id}"

  controlled_access_cidr_ingress = ["${var.controlled_access_cidr_ingress}"]
}

module "t2_compute" {
  source = "../../modules/dlami"
  name   = "jupyter-t2-${var.namespace}"

  key_name    = "${var.key_name}"
  bucket_name = "${var.s3_bucket_name}"

  spot_price = "0.1"

  default_environment = "pytorch_p36"

  vpc_id      = "${var.vpc_id}"
  subnet_list = "${var.subnets}"

  custom_security_groups = [
    "${var.efs_security_group_id}",
  ]

  efs_mount_id = "${var.efs_id}"

  controlled_access_cidr_ingress = ["${var.controlled_access_cidr_ingress}"]
}
