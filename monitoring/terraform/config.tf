data "aws_ssm_parameter" "grafana_admin_password" {
  name = "/aws/reference/secretsmanager/monitoring/grafana_admin_password"
}

data "aws_ssm_parameter" "bitly_access_token" {
  name = "/aws/reference/secretsmanager/monitoring/bitly_access_token"
}

data "aws_ssm_parameter" "critical_slack_webhook" {
  name = "/aws/reference/secretsmanager/monitoring/critical_slack_webhook"
}

data "aws_ssm_parameter" "noncritical_slack_webhook" {
  name = "/aws/reference/secretsmanager/monitoring/noncritical_slack_webhook"
}

locals {
  infra_bucket = "wellcomecollection-platform-infra"

  key_name = "wellcomedigitalplatform"

  admin_cidr_ingress = "${data.terraform_remote_state.infra_critical.admin_cidr_ingress}"

  grafana_anonymous_enabled = true
  grafana_anonymous_role    = "Viewer"
  grafana_admin_user        = "admin"
  grafana_admin_password    = "${data.aws_ssm_parameter.grafana_admin_password.value}"

  bitly_access_token = "${data.aws_ssm_parameter.bitly_access_token.value}"

  critical_slack_webhook    = "${data.aws_ssm_parameter.critical_slack_webhook.value}"
  noncritical_slack_webhook = "${data.aws_ssm_parameter.noncritical_slack_webhook.value}"
}
