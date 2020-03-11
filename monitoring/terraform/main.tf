module "monitoring-271118" {
  source = "./stack"

  namespace = "monitoring-271118"

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  vpc_id       = "${local.vpc_id}"

  efs_id                = "${module.grafana_efs.efs_id}"
  efs_security_group_id = "${aws_security_group.efs_security_group.id}"

  domain = "monitoring.wellcomecollection.org"

  public_subnets  = "${local.public_subnets}"
  private_subnets = "${local.private_subnets}"

  infra_bucket       = "${local.infra_bucket}"
  key_name           = "${local.key_name}"
  aws_region         = "${var.aws_region}"
  admin_cidr_ingress = "${local.admin_cidr_ingress}"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"

  # grafana

  grafana_admin_user        = "${local.grafana_admin_user}"
  grafana_anonymous_role    = "${local.grafana_anonymous_role}"
  grafana_admin_password    = "${local.grafana_admin_password}"
  grafana_anonymous_enabled = "${local.grafana_anonymous_enabled}"

  # post_to_slack

  dlq_alarm_arn                  = "${local.dlq_alarm_arn}"
  gateway_server_error_alarm_arn = "${local.gateway_server_error_alarm_arn}"
  cloudfront_errors_topic_arn    = "${local.cloudfront_errors_topic_arn}"
  critical_slack_webhook         = "${local.critical_slack_webhook}"
  non_critical_slack_webhook     = "${local.noncritical_slack_webhook}"
  bitly_access_token             = "${local.bitly_access_token}"

  # IAM

  allow_cloudwatch_read_metrics_policy_json = "${data.aws_iam_policy_document.allow_cloudwatch_read_metrics.json}"
  cloudwatch_allow_filterlogs_policy_json   = "${data.aws_iam_policy_document.cloudwatch_allow_filterlogs.json}"

  providers = {
    aws.us_east_1 = aws.us_east_1
  }
}
