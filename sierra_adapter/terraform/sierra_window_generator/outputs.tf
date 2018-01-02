output "queue_name" {
  description = "An SQS queue containing Sierra update windows"
  value       = "${module.windows_queue.name}"
}

output "queue_arn" {
  description = "An SQS queue containing Sierra update windows"
  value       = "${module.windows_queue.arn}"
}

output "queue_url" {
  description = "An SQS queue containing Sierra update windows"
  value       = "${module.windows_queue.id}"
}
