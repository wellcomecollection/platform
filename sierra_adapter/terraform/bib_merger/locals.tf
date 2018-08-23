locals {
  resource_type_singular = "${replace("${var.resource_type}", "s", "")}"
  container_image        = "${module.ecr_repository_sierra_merger.repository_url}:${var.release_id}"
}
