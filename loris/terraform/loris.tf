module "loris_service" {
  source = "service"

  namespace = "loris-delta"

  certificate_domain = "api.wellcomecollection.org"

  cpu    = "3960"
  memory = "7350"

  aws_region = "${var.aws_region}"

  vpc_id          = "${local.vpc_id}"
  private_subnets = "${local.private_subnets}"
  public_subnets  = "${local.public_subnets}"

  ssh_controlled_ingress_sg = "${local.ssh_controlled_ingress_sg}"
  key_name                  = "${var.key_name}"

  ebs_container_path = "/mnt/loris"

  healthcheck_path = "/image/"
  path_pattern     = "/*"

  sidecar_container_image = "${module.ecr_nginx_loris_delta.repository_url}:${var.release_ids["nginx_loris-delta"]}"
  sidecar_container_port  = "9000"

  sidecar_cpu      = "128"
  sidecar_memory   = "128"

  sidecar_env_vars = {}

  app_container_image = "${module.ecr_loris.repository_url}:${var.release_ids["loris"]}"
  app_container_port  = "8888"

  app_cpu      = "3076"
  app_memory   = "7222"

  app_env_vars = {}
}
