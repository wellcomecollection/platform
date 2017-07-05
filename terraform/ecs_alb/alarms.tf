resource "aws_cloudwatch_metric_alarm" "alb_500_errors" {
  alarm_name          = "${var.name}-alb-500-errors"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "HTTPCode_ELB_5XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = "60"
  statistic           = "Sum"
  threshold           = "1"

  dimensions {
    LoadBalancer = "${aws_alb.ecs_service.arn_suffix}"
  }

  alarm_description = "This metric monitors the ${var.name} ALB for 500s (Loris & API)"
  alarm_actions     = []
}

resource "aws_cloudwatch_metric_alarm" "target_500_errors" {
  alarm_name          = "${var.name}-target-500-errors"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "HTTPCode_Target_5XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = "60"
  statistic           = "Sum"
  threshold           = "1"

  dimensions {
    LoadBalancer = "${aws_alb.ecs_service.arn_suffix}"
  }

  alarm_description = "This metric monitors the ${var.name} ALB _target_ for 500s (API only)"
  alarm_actions     = []
}

resource "aws_cloudwatch_metric_alarm" "alb_400_errors" {
  alarm_name          = "${var.name}-alb-400-errors"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "HTTPCode_ELB_4XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = "60"
  statistic           = "Sum"
  threshold           = "1"

  dimensions {
    LoadBalancer = "${aws_alb.ecs_service.arn_suffix}"
  }

  alarm_description = "This metric monitors the ${var.name} ALB for 400s (Loris & API)"
  alarm_actions     = []
}

resource "aws_cloudwatch_metric_alarm" "target_400_errors" {
  alarm_name          = "${var.name}-target-400-errors"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "HTTPCode_Target_4XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = "60"
  statistic           = "Sum"
  threshold           = "1"

  dimensions {
    LoadBalancer = "${aws_alb.ecs_service.arn_suffix}"
  }

  alarm_description = "This metric monitors the ${var.name} ALB _target_ for 400s (API only)"
  alarm_actions     = []
}