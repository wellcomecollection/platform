module "ecs_gatling_iam" {
  source = "../terraform/ecs_iam"
  name   = "gatling"
}

module "ecs_grafana_iam" {
  source = "../terraform/ecs_iam"
  name   = "grafana"
}

module "ecs_monitoring_iam" {
  source = "../terraform/ecs_iam"
  name   = "monitoring"
}
