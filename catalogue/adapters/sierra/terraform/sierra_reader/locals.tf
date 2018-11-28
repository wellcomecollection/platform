locals {
  service_name    = "sierra_${var.resource_type}_reader"
  container_image = "${var.sierra_reader_ecr_repository_url}:${var.release_id}"
}
