locals {
  reindex_worker_container_image = "${module.ecr_repository_reindex_worker.repository_url}:${var.release_ids["reindex_worker"]}"
}
