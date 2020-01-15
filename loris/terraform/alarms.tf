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
    DistributionId = "${aws_cloudfront_distribution.loris.id}"
    Region         = "Global"
  }

  alarm_description = "Monitors 4xx errors from the Loris CloudFront distro"
  alarm_actions     = ["${aws_sns_topic.cloudfront_errors.arn}"]

  // This alarm action is disabled for now (2020-01-15) as it is being noisy
  // and drowning out our other alarms.
  // It should be re-enabled eventually: https://github.com/wellcometrust/platform/issues/4170
  actions_enabled = false

  provider = "aws.us_east_1"
}

resource "aws_sns_topic" "cloudfront_errors" {
  name     = "cloudfront_errors"
  provider = "aws.us_east_1"
}
