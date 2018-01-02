output "windows_queue_name" {
  description = "An SQS queue containing Sierra update windows"
  value       = "${module.windows_queue.name}"
}
