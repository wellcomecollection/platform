output "vhs_sourcedata_full_access_policy" {
  value = "${module.vhs_sourcedata.full_access_policy}"
}

output "vhs_sourcedata_dynamodb_update_policy" {
  value = "${module.vhs_sourcedata.dynamodb_update_policy}"
}

output "vhs_sourcedata_table_name" {
  value = "${module.vhs_sourcedata.table_name}"
}

output "vhs_sourcedata_table_stream_arn" {
  value = "${module.vhs_sourcedata.table_stream_arn}"
}

output "vhs_sourcedata_bucket_name" {
  value = "${module.vhs_sourcedata.bucket_name}"
}

output "vhs_goobi_full_access_policy" {
  value = "${module.vhs_goobi_mets.full_access_policy}"
}

output "vhs_goobi_table_name" {
  value = "${module.vhs_goobi_mets.table_name}"
}

output "vhs_goobi_bucket_name" {
  value = "${module.vhs_goobi_mets.bucket_name}"
}

output "vpc_services_id" {
  value = "${module.vpc_services.vpc_id}"
}

// TODO delete once the reindexer stack gets migrated to fargate
output "alb_cloudwatch_id" {
  value = "${module.catalogue_pipeline_cluster.alb_cloudwatch_id}"
}

output "alb_listener_https_arn" {
  value = "${module.catalogue_pipeline_cluster.alb_listener_https_arn}"
}

output "alb_listener_http_arn" {
  value = "${module.catalogue_pipeline_cluster.alb_listener_http_arn}"
}

//

output "cluster_name" {
  value = "${module.catalogue_pipeline_cluster.cluster_name}"
}
