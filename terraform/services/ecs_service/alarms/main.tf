resource "aws_cloudwatch_metric_alarm" "alb_alarm" {
  count = "${var.enable_alb_alarm}"

  alarm_name          = "${var.name}"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "${var.metric}"
  namespace           = "AWS/ApplicationELB"
  period              = "60"
  statistic           = "Sum"
  threshold           = "1"

  dimensions {
    LoadBalancer = "${var.lb_dimension}"
    TargetGroup  = "${var.tg_dimension}"
  }

  alarm_description = "This metric monitors ${var.name}"
  alarm_actions     = ["${var.topic_arn}"]
}
