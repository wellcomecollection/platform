resource "aws_cloudwatch_metric_alarm" "queue_high" {
  alarm_name          = "${var.name}-queue_high"
  comparison_operator = "GreaterThanOrEqualToThreshold"

  evaluation_periods = "${var.high_period_in_minutes}"

  datapoints_to_alarm = "${var.high_period_in_minutes}"
  namespace           = "AWS/SQS"
  metric_name         = "ApproximateNumberOfMessagesVisible"

  dimensions {
    QueueName = "${var.queue_name}"
  }

  statistic = "Maximum"

  period = "60"

  threshold = "${var.high_threshold}"

  treat_missing_data = "missing"

  alarm_actions = [
    "${var.scale_up_arn}",
  ]
}

resource "aws_cloudwatch_metric_alarm" "queue_low" {
  alarm_name          = "${var.name}-queue_low"
  comparison_operator = "LessThanThreshold"

  evaluation_periods = "${var.low_period_in_minutes}"

  datapoints_to_alarm = "${var.low_period_in_minutes}"
  namespace           = "queues/visible_and_in_flight"
  metric_name         = "${var.queue_name}"

  statistic = "Sum"

  period = "60"

  threshold = "${var.low_threshold}"

  treat_missing_data = "missing"

  alarm_actions = [
    "${var.scale_down_arn}",
  ]
}
