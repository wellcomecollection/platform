module "loris_userdata" {
  source                             = "git::https://github.com/wellcometrust/terraform.git//userdata?ref=v6.4.3"
  cluster_name                       = "${aws_ecs_cluster.loris.name}"
  ebs_block_device                   = "/dev/xvdb"
  cache_cleaner_cloudwatch_log_group = "${aws_cloudwatch_log_group.cache_cleaner_log_group.name}"
  ebs_cache_max_age_days             = "30"
  ebs_cache_max_size                 = "160G"
}
