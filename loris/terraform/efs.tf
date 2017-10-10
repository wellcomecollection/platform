module "loris_efs" {
  name                         = "loris_cache"
  source                       = "git::https://github.com/wellcometrust/terraform.git//efs?ref=v1.0.0"
  vpc_id                       = "${local.vpc_api_id}"
  subnets                      = "${local.vpc_api_subnets}"
  efs_access_security_group_id = "${module.loris_cluster_asg.instance_sg_id}"
  performance_mode             = "maxIO"
}
