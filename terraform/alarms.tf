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
