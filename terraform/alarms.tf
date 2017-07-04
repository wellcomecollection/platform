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

resource "aws_cloudwatch_metric_alarm" "api_alb_500_errors" {
  alarm_name          = "api-alb-500-errors"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "HTTPCode_ELB_5XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = "60"
  statistic           = "Sum"
  threshold           = "1"

  dimensions {
    LoadBalancer = "${module.api_alb.cloudwatch_id}"
  }

  alarm_description = "This metric monitors the API ALB for 500s (Loris & API)"
  alarm_actions     = []
}

resource "aws_cloudwatch_metric_alarm" "api_target_500_errors" {
  alarm_name          = "api-target-500-errors"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "HTTPCode_Target_5XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = "60"
  statistic           = "Sum"
  threshold           = "1"

  dimensions {
    LoadBalancer = "${module.api_alb.cloudwatch_id}"
  }

  alarm_description = "This metric monitors the API ALB _target_ for 500s (API only)"
  alarm_actions     = []
}

resource "aws_cloudwatch_metric_alarm" "api_alb_400_errors" {
  alarm_name          = "api-alb-400-errors"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "HTTPCode_ELB_4XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = "60"
  statistic           = "Sum"
  threshold           = "1"

  dimensions {
    LoadBalancer = "${module.api_alb.cloudwatch_id}"
  }

  alarm_description = "This metric monitors the API ALB for 400s (Loris & API)"
  alarm_actions     = []
}

resource "aws_cloudwatch_metric_alarm" "api_target_400_errors" {
  alarm_name          = "api-target-400-errors"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "HTTPCode_Target_4XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = "60"
  statistic           = "Sum"
  threshold           = "1"

  dimensions {
    LoadBalancer = "${module.api_alb.cloudwatch_id}"
  }

  alarm_description = "This metric monitors the API ALB _target_ for 400s (API only)"
  alarm_actions     = []
}