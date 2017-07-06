module "id_minter_topic" {
  source = "./sns"
  name   = "id_minter"
}

module "es_ingest_topic" {
  source = "./sns"
  name   = "es_ingest"
}

module "miro_transformer_topic" {
  source = "./sns"
  name   = "miro_transformer"
}

module "calm_transformer_topic" {
  source = "./sns"
  name   = "calm_transformer"
}

module "service_scheduler_topic" {
  source = "./sns"
  name   = "service_scheduler"
}

module "dynamo_capacity_topic" {
  source = "./sns"
  name   = "dynamo_capacity_requests"
}

module "ec2_terminating_topic" {
  source = "./sns"
  name   = "ec2_terminating_topic"
}

module "old_deployments" {
  source = "./sns"
  name   = "old_deployments"
}

# Alarm topics

module "dlq_alarm" {
  source = "./sns"
  name   = "dlq_alarm"
}

module "transformer_dlq_alarm" {
  source = "./sns"
  name   = "transformer_dlq_alarm"
}

module "ec2_instance_terminating_for_too_long_alarm" {
  source = "./sns"
  name   = "ec2_instance_terminating_for_too_long_alarm"
}

module "alb_server_error_alarm" {
  source = "./sns"
  name   = "alb_server_error_alarm"
}

module "alb_client_error_alarm" {
  source = "./sns"
  name   = "alb_client_error_alarm"
}

module "lambda_error_alarm" {
  source = "./sns"
  name   = "lambda_error_alarm"
}
