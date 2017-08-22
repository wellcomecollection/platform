# One repository per application (see http://stackoverflow.com/a/37543992 and https://github.com/docker/docker/blob/master/image/spec/v1.2.md)

# Kept around for migration purposes.  TODO: Retire this repo.
module "ecr_repository_nginx" {
  source = "./ecr"
  name   = "nginx"
}

module "ecr_repository_nginx_api" {
  source = "./ecr"
  name   = "nginx_api"
}

module "ecr_repository_nginx_grafana" {
  source = "./ecr"
  name   = "nginx_grafana"
}

module "ecr_repository_nginx_loris" {
  source = "./ecr"
  name   = "nginx_loris"
}

module "ecr_repository_nginx_services" {
  source = "./ecr"
  name   = "nginx_services"
}

module "ecr_repository_api" {
  source = "./ecr"
  name   = "api"
}

module "ecr_repository_transformer" {
  source = "./ecr"
  name   = "transformer"
}

module "ecr_repository_id_minter" {
  source = "./ecr"
  name   = "id_minter"
}

module "ecr_repository_ingestor" {
  source = "./ecr"
  name   = "ingestor"
}

module "ecr_repository_reindexer" {
  source = "./ecr"
  name   = "reindexer"
}

module "ecr_repository_loris" {
  source = "./ecr"
  name   = "loris"
}

module "ecr_repository_cache_cleaner" {
  source = "./ecr"
  name   = "cache_cleaner"
}

module "ecr_repository_gatling" {
  source = "./ecr"
  name   = "gatling"
}

module "ecr_repository_tif-metadata" {
  source = "./ecr"
  name   = "tif-metadata"
}

module "ecr_repository_miro_adapter" {
  source = "./ecr"
  name   = "miro_adapter"
}

module "ecr_repository_elasticdump" {
  source = "./ecr"
  name   = "elasticdump"
}
