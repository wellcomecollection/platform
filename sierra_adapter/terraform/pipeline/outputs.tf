output "merger_repository_url" {
  value = "${module.ecr_repository_merger.repository_url}"
}

output "to_dynamo_repository_url" {
  value = "${module.ecr_repository_to_dynamo.repository_url}"
}
