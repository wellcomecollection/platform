module "loris-2019_01_30" {
  source = "stack"

  namespace = "loris-2019-01-30"

  certificate_domain = "api.wellcomecollection.org"

  aws_region = "${var.aws_region}"

  vpc_id          = "${local.vpc_id}"
  private_subnets = "${local.private_subnets}"
  public_subnets  = "${local.public_subnets}"

  sidecar_container_image = "${local.nginx_loris_release_uri}"
  app_container_image     = "${local.loris_release_uri}"

  asg_desired        = 4
  task_desired_count = 4

  # The cache cleaner is published from the dockerfiles repo, and so is versioned
  # separately from ECR.  We pin it to a specific release so it can't change
  # under our feet.
  # See: https://github.com/wellcometrust/dockerfiles
  ebs_cache_cleaner_daemon_image_version = "39"

  ebs_cache_cleaner_daemon_max_size_in_gb = "1"
  ebs_cache_cleaner_daemon_clean_interval = "1m"
}
