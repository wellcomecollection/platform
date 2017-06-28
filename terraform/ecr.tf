# One repository per application (see http://stackoverflow.com/a/37543992 and https://github.com/docker/docker/blob/master/image/spec/v1.2.md)

module "ecr_repository_nginx" {
  source = "./ecr"
  name   = "nginx"
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

module "ecr_repository_calm_adapter" {
  source = "./ecr"
  name   = "calm_adapter"
}

module "ecr_repository_ingestor" {
  source = "./ecr"
  name   = "ingestor"
}

module "ecr_repository_reindexer" {
  source = "./ecr"
  name   = "reindexer"
}
