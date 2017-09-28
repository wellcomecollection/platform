# One repository per application (see http://stackoverflow.com/a/37543992 and https://github.com/docker/docker/blob/master/image/spec/v1.2.md)

module "ecr_repository_nginx_api" {
  source = "../terraform/ecr"
  name   = "nginx_api"
}

module "ecr_repository_nginx_services" {
  source = "../terraform/ecr"
  name   = "nginx_services"
}

module "ecr_repository_api" {
  source = "../terraform/ecr"
  name   = "api"
}

module "ecr_repository_transformer" {
  source = "../terraform/ecr"
  name   = "transformer"
}

module "ecr_repository_id_minter" {
  source = "../terraform/ecr"
  name   = "id_minter"
}

module "ecr_repository_ingestor" {
  source = "../terraform/ecr"
  name   = "ingestor"
}

module "ecr_repository_reindexer" {
  source = "../terraform/ecr"
  name   = "reindexer"
}

module "ecr_repository_cache_cleaner" {
  source = "../terraform/ecr"
  name   = "cache_cleaner"
}

module "ecr_repository_tif-metadata" {
  source = "../terraform/ecr"
  name   = "tif-metadata"
}

module "ecr_repository_elasticdump" {
  source = "../terraform/ecr"
  name   = "elasticdump"
}

module "ecr_repository_update_api_docs" {
  source = "../terraform/ecr"
  name   = "update_api_docs"
}
