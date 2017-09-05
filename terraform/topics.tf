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

module "ec2_terminating_topic" {
  source = "./sns"
  name   = "ec2_terminating_topic"
}

# Alarm topics

module "ec2_instance_terminating_for_too_long_alarm" {
  source = "./sns"
  name   = "ec2_instance_terminating_for_too_long_alarm"
}

module "dlq_alarm" {
  source = "./sns"
  name   = "dlq_alarm"
}

module "transformer_dlq_alarm" {
  source = "./sns"
  name   = "transformer_dlq_alarm"
}

module "alb_server_error_alarm" {
  source = "./sns"
  name   = "alb_server_error_alarm"
}

module "alb_client_error_alarm" {
  source = "./sns"
  name   = "alb_client_error_alarm"
}

module "terminal_failure_alarm" {
  source = "./sns"
  name   = "terminal_failure_alarm"
}

module "load_test_results" {
  source = "./sns"
  name   = "load_test_results"
}

module "load_test_failure_alarm" {
  source = "./sns"
  name   = "load_test_failure_alarm"
}

module "terraform_apply_topic" {
  source = "./sns"
  name   = "terraform_apply"
}
