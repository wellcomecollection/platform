module "reindex_worker" {
  source = "./reindex_worker"

  reindexer_jobs            = "${local.reindexer_jobs}"
  reindexer_job_config_json = "${local.reindex_job_config_json}"

  reindex_worker_container_image = "${local.reindex_worker_container_image}"

  ecs_cluster_name = "${aws_ecs_cluster.cluster.name}"
  ecs_cluster_id   = "${aws_ecs_cluster.cluster.id}"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.reindexer.id}"

  account_id = "${data.aws_caller_identity.current.account_id}"
}
