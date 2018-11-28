module "stack-281118" {
  source = "stack"

  namespace = "datascience-281118"

  vpc_id         = "${local.vpc_id}"
  vpc_cidr_block = "${data.aws_vpc.datascience.cidr_block}"

  key_name = "${var.key_name}"
  aws_region = "${var.aws_region}"

  public_subnets  = "${local.public_subnets}"
  private_subnets = "${local.private_subnets}"

  notebook_bucket_name = "${aws_s3_bucket.jupyter.bucket}"
  notebook_bucket_arn  = "${aws_s3_bucket.jupyter.arn}"

  efs_id                = "${module.efs.efs_id}"
  efs_security_group_id = "${aws_security_group.efs_security_group.id}"

  admin_cidr_ingress = "${var.admin_cidr_ingress}"
}