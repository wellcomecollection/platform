module "loris-031218" {
  source = "stack"

  namespace = "loris-031218"

  certificate_domain = "api.wellcomecollection.org"

  aws_region = "${var.aws_region}"

  vpc_id          = "${local.vpc_id_new}"
  private_subnets = "${local.private_subnets_new}"
  public_subnets  = "${local.public_subnets_new}"

  key_name = "${var.key_name}"

  sidecar_container_image = "${module.ecr_nginx_loris_delta.repository_url}:${var.release_ids["nginx_loris-delta"]}"
  app_container_image     = "${module.ecr_loris.repository_url}:${var.release_ids["loris"]}"

  asg_desired        = 4
  task_desired_count = 4

  ebs_cache_cleaner_daemon_max_size_in_gb = "1"
  ebs_cache_cleaner_daemon_clean_interval = "1m"
}
