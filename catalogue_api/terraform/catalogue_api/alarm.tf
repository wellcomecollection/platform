resource "aws_cloudwatch_metric_alarm" "5xx_alarm" {
  alarm_name          = "catalogue-api-${var.production_api}-5xx-alarm"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "5XXError"
  namespace           = "AWS/ApiGateway"
  period              = "60"
  statistic           = "Sum"
  threshold           = "0"

  dimensions {
    ApiName = "${aws_api_gateway_rest_api.api.name}"
    Stage   = "${var.production_api}"
  }

  alarm_actions = ["${var.alarm_topic_arn}"]
}
