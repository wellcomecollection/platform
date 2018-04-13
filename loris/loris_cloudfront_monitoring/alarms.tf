# In general a 4xx isn't a significant event, but a significant number of
# 4xx errors may indicate a systemic problem with Loris (e.g. accidentally
# deleting all the images in the S3 bucket!).
#
# So we look for an unusually high number of CloudFront 4xxs.
#
# This is a finger-in-the-air estimate based on historic CloudFront stats.
#
resource "aws_cloudwatch_metric_alarm" "cloudfront_4xx_alarm" {
  alarm_name = "loris-cloudfront_4xx_error"

  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "4xxErrorRate"
  namespace           = "AWS/CloudFront"
  period              = "60"
  statistic           = "Sum"
  threshold           = "2500"
  treat_missing_data  = "breaching"

  dimensions {
    DistributionId = "${local.loris_cloudfront_id}"
    Region         = "Global"
  }

  alarm_description = "Monitors 4xx errors from the Loris CloudFront distro"
  alarm_actions = ["${module.cloudfront_errors_topic.arn}"]
}
