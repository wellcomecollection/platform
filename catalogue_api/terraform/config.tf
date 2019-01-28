locals {
  infra_bucket           = "${data.aws_ssm_parameter.infra_bucket.value}"
  critical_slack_webhook = "${data.aws_ssm_parameter.critical_slack_webhook.value}"

  es_cluster_credentials {
    name     = "${data.aws_ssm_parameter.es_cluster_credentials_name.value}"
    region   = "${data.aws_ssm_parameter.es_cluster_credentials_region.value}"
    port     = "${data.aws_ssm_parameter.es_cluster_credentials_port.value}"
    username = "${data.aws_ssm_parameter.es_cluster_credentials_username.value}"
    password = "${data.aws_ssm_parameter.es_cluster_credentials_password.value}"
    protocol = "${data.aws_ssm_parameter.es_cluster_credentials_protocol.value}"
  }
}

data "aws_ssm_parameter" "infra_bucket" {
  name = "/infra_shared/config/prod/infra_bucket"
}

data "aws_ssm_parameter" "critical_slack_webhook" {
  name = "/infra_shared/secrets/prod/critical_slack_webhook"
}

data "aws_ssm_parameter" "es_cluster_credentials_name" {
  name = "/catalogue/secrets/prod/es_cluster_credentials_name"
}

data "aws_ssm_parameter" "es_cluster_credentials_region" {
  name = "/catalogue/secrets/prod/es_cluster_credentials_region"
}

data "aws_ssm_parameter" "es_cluster_credentials_port" {
  name = "/catalogue/secrets/prod/es_cluster_credentials_port"
}

data "aws_ssm_parameter" "es_cluster_credentials_username" {
  name = "/catalogue/secrets/prod/es_cluster_credentials_username"
}

data "aws_ssm_parameter" "es_cluster_credentials_password" {
  name = "/catalogue/secrets/prod/es_cluster_credentials_password"
}

data "aws_ssm_parameter" "es_cluster_credentials_protocol" {
  name = "/catalogue/secrets/prod/es_cluster_credentials_protocol"
}
