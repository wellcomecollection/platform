module "ecs_services_iam" {
  source = "./ecs_iam"
  name   = "services"
}

module "ecs_calm_adapter_iam" {
  source = "./ecs_iam"
  name   = "calm_adapter"
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
