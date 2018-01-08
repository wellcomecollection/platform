locals {
  is_prod_api = "${var.name == var.prod_api ? true : false}"

  desired_count           = "${local.is_prod_api ? 3 : 1}"
  minimum_healthy_percent = "${local.is_prod_api ? 50 : 0}"

  enable_alb_alarm = "${local.is_prod_api ? 1 : 0}"

  api_release_id   = "${local.is_prod_api ? var.prod_api_release_id : var.release_ids["api"]}"
  nginx_release_id = "${local.is_prod_api ? var.prod_api_nginx_release_id : var.release_ids["nginx_api"]}"

  host_name = "${local.is_prod_api ? api.wellcomecollection.org : api-stage.wellcomecollection.org}"
}
