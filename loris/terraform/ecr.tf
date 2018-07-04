module "ecr_nginx_loris_delta" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "nginx_loris-delta"
}

module "ecr_loris" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "loris"
}
