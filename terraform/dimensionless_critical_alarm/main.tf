resource "aws_cloudwatch_metric_alarm" "dimensionless_critical" {
  alarm_name          = "${var.name}"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "${var.metric_name}"
  namespace           = "${var.namespace}"
  period              = "60"
  statistic           = "Sum"
  threshold           = "1"

  alarm_description = "This metric monitors  service for terminal failure"
  alarm_actions     = ["${var.alarm_action_arn}"]
}

