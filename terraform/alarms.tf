resource "aws_cloudwatch_metric_alarm" "notify_old_deploys" {
  alarm_name          = "notify-old-deploys"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "NumberOfMessagesPublished"
  namespace           = "AWS/SNS"
  period              = "60"
  statistic           = "Sum"
  threshold           = "1"

  dimensions {
    TopicName = "${module.old_deployments.name}"
  }

  alarm_description = "This metric monitors alerts for old deploys"
  alarm_actions     = []
}

module "transformer_trybackoff" {
  source           = "./dimensionless_critical_alarm"
  metric_name      = "TransformerWorkerService_TerminalFailure"
  name             = "TransformerWorkerService_TerminalFailure"
  namespace        = "miro-transformer"
  alarm_action_arn = "${module.terminal_failure_alarm.arn}"
}

module "ingestor_trybackoff" {
  source           = "./dimensionless_critical_alarm"
  metric_name      = "IngestorWorkerService_TerminalFailure"
  name             = "IngestorWorkerService_TerminalFailure"
  namespace        = "ingestor"
  alarm_action_arn = "${module.terminal_failure_alarm.arn}"
}

module "id-minter_trybackoff" {
  source           = "./dimensionless_critical_alarm"
  metric_name      = "IdMinterWorkerService_TerminalFailure"
  name             = "IdMinterWorkerService_TerminalFailure"
  namespace        = "id-minter"
  alarm_action_arn = "${module.terminal_failure_alarm.arn}"
}
