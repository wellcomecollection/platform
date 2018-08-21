locals {
  resource_type_singular = "${replace("${var.resource_type}", "s", "")}"
  container_image        = "${module.ecr_repository_sierra_merger.repository_url}:20e11e803698059bbec56d73b6d653a2c01c0e50"
}
