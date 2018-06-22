module "p2_compute" {
  source = "git::https://github.com/wellcometrust/terraform.git//ec2/prebuilt/dlami?ref=ecs_v2"
  name   = "jupyter-notebook-p2"

  key_name    = "${var.key_name}"
  bucket_name = "${var.s3_bucket_name}"

  instance_type = "p2.xlarge"
  spot_price    = "0.5"

  default_environment = "tensorflow_p36"

  vpc_id      = "${var.vpc_id}"
  subnet_list = "${var.subnets}"
}

module "t2_compute" {
  source = "git::https://github.com/wellcometrust/terraform.git//ec2/prebuilt/dlami?ref=ecs_v2"
  name   = "jupyter-t2"

  key_name    = "${var.key_name}"
  bucket_name = "${var.s3_bucket_name}"

  spot_price = "0.4"

  default_environment = "tensorflow_p36"

  vpc_id      = "${var.vpc_id}"
  subnet_list = "${var.subnets}"
}
