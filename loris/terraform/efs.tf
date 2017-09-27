module "loris_efs" {
  name                         = "loris_cache_2"
  source                       = "../../terraform/efs"
  vpc_id                       = "${data.terraform_remote_state.platform.vpc_api_id}"
  subnets                      = "${data.terraform_remote_state.platform.vpc_api_subnets}"
  efs_access_security_group_id = "${module.loris_cluster_asg.instance_sg_id}"
  performance_mode             = "maxIO"
}