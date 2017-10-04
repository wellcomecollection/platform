module "loris_userdata" {
  source            = "../../terraform/userdata"
  cluster_name      = "${aws_ecs_cluster.loris.name}"
  efs_filesystem_id = "${module.loris_efs.efs_id}"
}

module "loris_userdata_ebs" {
  source                             = "../../terraform/userdata"
  cluster_name                       = "${aws_ecs_cluster.loris_ebs.name}"
  ebs_block_device                   = "/dev/xvdb"
  cache_cleaner_cloudwatch_log_group = "${aws_cloudwatch_log_group.cache_cleaner_log_group.name}"
  ebs_cache_max_age_days             = "30"
  ebs_cache_max_size                 = "160G"
}
