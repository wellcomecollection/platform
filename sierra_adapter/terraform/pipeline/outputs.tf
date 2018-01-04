output "merger_repository_url" {
  value = "${module.ecr_repository_merger.repository_url}"
}

output "to_dynamo_repository_url" {
  value = "${module.ecr_repository_to_dynamo.repository_url}"
}

output "windows_queue_name" {
  value = "${module.windows_queue.name}"
}

output "windows_queue_arn" {
  value = "${module.windows_queue.arn}"
}

output "windows_queue_id" {
  value = "${module.windows_queue.id}"
}
