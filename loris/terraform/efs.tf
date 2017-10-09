module "loris_efs" {
  name                         = "loris_cache"
  source                       = "git::https://github.com/wellcometrust/terraform.git//efs?ref=v1.0.0"
  vpc_id                       = "${data.terraform_remote_state.platform.vpc_api_id}"
  subnets                      = "${data.terraform_remote_state.platform.vpc_api_subnets}"
  efs_access_security_group_ids = ["${module.loris_cluster_asg.instance_sg_id}","${module.loris_cluster_asg_m4.instance_sg_id}"]
  performance_mode             = "maxIO"
}
