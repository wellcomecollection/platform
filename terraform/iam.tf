module "ecs_services_iam" {
  source = "./ecs_iam"
  name   = "services"
}

module "ecs_monitoring_iam" {
  source = "./ecs_iam"
  name   = "monitoring"
}

module "ecs_calm_adapter_iam" {
  source = "./ecs_iam"
  name   = "calm_adapter"
}

module "ecs_miro_reindexer_iam" {
  source = "./ecs_iam"
  name   = "miro_reindexer"
}

module "ecs_transformer_iam" {
  source = "./ecs_iam"
  name   = "transformer"
}

module "ecs_id_minter_iam" {
  source = "./ecs_iam"
  name   = "id_minter"
}

module "ecs_ingestor_iam" {
  source = "./ecs_iam"
  name   = "ingestor"
}

module "ecs_api_iam" {
  source = "./ecs_iam"
  name   = "api"
}

module "ecs_loris_iam" {
  source = "./ecs_iam"
  name   = "loris"
}

module "ecs_grafana_iam" {
  source = "./ecs_iam"
  name   = "grafana"
}

module "ecs_cache_cleaner_iam" {
  source = "./ecs_iam"
  name   = "cache_cleaner"
}

module "ecs_gatling_iam" {
  source = "./ecs_iam"
  name   = "gatling"
}
