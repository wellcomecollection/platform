module "transformer_trybackoff" {
  source           = "git::https://github.com/wellcometrust/terraform.git//dimensionless_critical_alarm?ref=v1.0.0"
  metric_name      = "TransformerWorkerService_TerminalFailure"
  name             = "TransformerWorkerService_TerminalFailure"
  namespace        = "miro-transformer"
  alarm_action_arn = "${local.terminal_failure_alarm_arn}"
}

module "ingestor_trybackoff" {
  source           = "git::https://github.com/wellcometrust/terraform.git//dimensionless_critical_alarm?ref=v1.0.0"
  metric_name      = "IngestorWorkerService_TerminalFailure"
  name             = "IngestorWorkerService_TerminalFailure"
  namespace        = "ingestor"
  alarm_action_arn = "${local.terminal_failure_alarm_arn}"
}

module "id-minter_trybackoff" {
  source           = "git::https://github.com/wellcometrust/terraform.git//dimensionless_critical_alarm?ref=v1.0.0"
  metric_name      = "IdMinterWorkerService_TerminalFailure"
  name             = "IdMinterWorkerService_TerminalFailure"
  namespace        = "id-minter"
  alarm_action_arn = "${local.terminal_failure_alarm_arn}"
}
