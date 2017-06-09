output "name" {
  description = "Name of the created SNS topic"
  value       = "${aws_sns_topic.topic.name}"
}

output "arn" {
  description = "ARN for the created SNS topic"
  value       = "${aws_sns_topic.topic.arn}"
}

output "publish_policy" {
  description = "Policy that allows publishing to the created SNS topic"
  value       = "${data.aws_iam_policy_document.publish_to_topic.json}"
}
