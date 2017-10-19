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
