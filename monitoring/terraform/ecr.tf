module "ecr_repository_gatling" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "gatling"
}

module "ecr_repository_nginx_grafana" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "nginx_grafana"
}
