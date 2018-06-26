locals {
  service_name = "sierra_${var.resource_type}_reader"
  container_image ="${module.ecr_repository_sierra_reader.repository_url}:${var.release_id}"
}