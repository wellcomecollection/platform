# In general a 4xx isn't a significant event, but a significant number of
# 4xx errors may indicate a systemic problem with Loris (e.g. accidentally
# deleting all the images in the S3 bucket!).
#
# So we look for an unusually high proportion of CloudFront 4xxs.
#
# In the last three weeks before our last incident, we'd never seen more than
# 1% of requests fail with a 4xx error, so 1.5% is a rough estimate of an
# "unusual" number of requests.
#
resource "aws_cloudwatch_metric_alarm" "cloudfront_4xx_alarm" {
  alarm_name = "loris-cloudfront_4xx_error"

  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "4xxErrorRate"
  namespace           = "AWS/CloudFront"
  period              = "60"
  statistic           = "Sum"
  threshold           = "1.5"
  treat_missing_data  = "breaching"

  dimensions {
    DistributionId = "${local.loris_cloudfront_id}"
    Region         = "Global"
  }
}
