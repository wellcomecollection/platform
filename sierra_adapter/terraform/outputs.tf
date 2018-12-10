output "merged_items_topic_name" {
  value = "${module.items_merger.topic_name}"
}

output "merged_items_topic_arn" {
  value = "${module.items_merger.topic_arn}"
}

output "merged_bibs_topic_name" {
  value = "${module.bibs_merger.topic_name}"
}

output "merged_bibs_topic_arn" {
  value = "${module.bibs_merger.topic_arn}"
}
