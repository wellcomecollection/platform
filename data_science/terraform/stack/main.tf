module "harrison_pim_notebook" {
  source = "notebooks"

  namespace = "${var.namespace}-notebook"

  s3_bucket_name = "${var.notebook_bucket_name}"
  s3_bucket_arn  = "${var.notebook_bucket_arn}"

  key_name = "${var.key_name}"

  subnets = "${var.public_subnets}"
  vpc_id  = "${var.vpc_id}"

  controlled_access_cidr_ingress = ["${var.admin_cidr_ingress}"]

  efs_id                = "${var.efs_id}"
  efs_security_group_id = "${var.efs_security_group_id}"
}

module "harrison_pim_notebook_v2" {
  source = "notebooks-pepperami"

  namespace = "${var.namespace}-notebook"

  s3_bucket_name = "${var.notebook_bucket_name}"
  s3_bucket_arn  = "${var.notebook_bucket_arn}"

  key_name = "${var.key_name}"

  vpc_id  = "${var.vpc_id}"

  controlled_access_cidr_ingress = ["${var.admin_cidr_ingress}"]

  efs_id                = "${var.efs_id}"
  efs_security_group_id = "${var.efs_security_group_id}"
}

module "labs" {
  source = "labs"

  namespace = "${var.namespace}-labs"

  vpc_id = "${var.vpc_id}"

  private_subnets = "${var.private_subnets}"
  public_subnets  = "${var.public_subnets}"
}
