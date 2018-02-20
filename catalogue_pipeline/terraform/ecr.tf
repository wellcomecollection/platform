module "ecr_repository_nginx_services" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "nginx_services"
}

module "ecr_repository_transformer" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "transformer"
}

module "ecr_repository_id_minter" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "id_minter"
}

module "ecr_repository_ingestor" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "ingestor"
}

module "ecr_repository_elasticdump" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "elasticdump"
}
