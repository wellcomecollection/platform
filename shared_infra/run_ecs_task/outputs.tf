output "arn" {
  description = "ARN for the SNS topic"
  value       = "${module.run_ecs_task_topic.arn}"
}

output "publish_policy" {
  description = "Policy that allows publishing to the SNS topic"
  value       = "${module.run_ecs_task_topic.publish_policy}"
}
