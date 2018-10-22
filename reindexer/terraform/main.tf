module "source_data_reindex_catalogue_pipeline" {
  source = "./source_data_reindex"

  reindex_worker_container_image = "${local.reindex_worker_container_image}"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  namespace    = "catalogue_pipeline"
}

module "source_data_reindex_reporting_pipeline" {
  source = "./source_data_reindex"

  reindex_worker_container_image = "${local.reindex_worker_container_image}"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  namespace    = "reporting_pipeline"
}
