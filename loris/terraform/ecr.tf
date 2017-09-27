module "ecr_nginx_loris" {
  source = "../../terraform/ecr"
  name   = "nginx_loris"
}

module "ecr_loris" {
  source = "../../terraform/ecr"
  name   = "loris"
}
