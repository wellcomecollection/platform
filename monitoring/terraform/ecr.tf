module "ecr_repository_gatling" {
  source = "../terraform/ecr"
  name   = "gatling"
}

module "ecr_repository_nginx_grafana" {
  source = "../terraform/ecr"
  name   = "nginx_grafana"
}
