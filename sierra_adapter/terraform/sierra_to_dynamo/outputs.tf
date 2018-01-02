output "queue_name" {
  value = "${module.queue_sierra_updates.name}"
}

output "queue_arn" {
  value = "${module.queue_sierra_updates.arn}"
}

output "queue_url" {
  value = "${module.queue_sierra_updates.id}"
}
