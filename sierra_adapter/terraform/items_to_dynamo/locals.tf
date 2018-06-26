locals {
  container_image ="${module.ecr_repository_sierra_to_dynamo.repository_url}:${var.release_id}"
}
