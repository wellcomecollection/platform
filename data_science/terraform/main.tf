module "p2_compute" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//dlami_asg?ref=fix-userdata"
  name   = "jupyter-p2"

  key_name    = "${var.key_name}"
  bucket_name = "${aws_s3_bucket.jupyter.id}"

  instance_type = "p2.xlarge"

  vpc_id      = "${module.vpc.vpc_id}"
  vpc_subnets = "${module.vpc.subnets}"

  default_environment = "tensorflow_p36"
}

module "t2_compute" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//dlami_asg?ref=fix-userdata"
  name   = "jupyter-t2"

  key_name    = "${var.key_name}"
  bucket_name = "${aws_s3_bucket.jupyter.id}"

  vpc_id      = "${module.vpc.vpc_id}"
  vpc_subnets = "${module.vpc.subnets}"

  default_environment = "tensorflow_p36"
}
