module "harrison_pim_notebook" {
  source = "notebooks"

  namespace = "noteboook"

  s3_bucket_name = "${aws_s3_bucket.jupyter.id}"
  s3_bucket_arn  = "${aws_s3_bucket.jupyter.arn}"

  key_name = "${var.key_name}"

  aws_region = "${var.aws_region}"

  vpc_cidr_block = "${var.vpc_cidr_block}"
  subnets        = "${module.network.public_subnets}"
  vpc_id         = "${module.network.vpc_id}"

  controlled_access_cidr_ingress = ["${var.admin_cidr_ingress}"]
}

module "labs" {
  source = "labs"

  namespace = "datalabs"

  vpc_cidr_block = "${var.vpc_cidr_block}"

  vpc_id = "${module.network.vpc_id}"

  private_subnets = "${module.network.private_subnets}"
  public_subnets  = "${module.network.public_subnets}"
}
