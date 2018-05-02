module "p2_compute" {
  source = "git::https://github.com/wellcometrust/terraform.git//terraform-modules/dlami_asg?ref=fix-userdata"
  name   = "jupyter-p2"

  key_name    = "${var.key_name}"
  bucket_name = "${aws_s3_bucket.jupyter.id}"

  instance_type = "p2.xlarge"

  vpc_id      = "${module.vpc.vpc_id}"
  vpc_subnets = "${module.vpc.subnets}"
}

module "t2_compute" {
  source = "git::https://github.com/wellcometrust/terraform.git//terraform-modules/dlami_asg?ref=fix-userdata"
  name   = "jupyter-t2"

  key_name    = "${var.key_name}"
  bucket_name = "${aws_s3_bucket.jupyter.id}"

  vpc_id      = "${module.vpc.vpc_id}"
  vpc_subnets = "${module.vpc.subnets}"
}
