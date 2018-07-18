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

output "cluster_name" {
  value = "${module.catalogue_pipeline.cluster_name}"
}
