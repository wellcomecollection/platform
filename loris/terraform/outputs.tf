output "cloudfront_errors_topic_arn" {
  value = "${aws_sns_topic.cloudfront_errors.arn}"
}
