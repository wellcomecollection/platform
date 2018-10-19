# Catalogue pipeline

output "miro_topic_name" {
  value = "${module.source_data_reindex_catalogue_pipeline.miro_topic_name}"
}

output "sierra_topic_name" {
  value = "${module.source_data_reindex_catalogue_pipeline.sierra_topic_name}"
}

output "sierra_items_topic_name" {
  value = "${module.source_data_reindex_catalogue_pipeline.sierra_items_topic_name}"
}

# Reporting pipeline

output "miro_topic_name_reporting" {
  value = "${module.source_data_reindex_reporting_pipeline.miro_topic_name}"
}

output "sierra_topic_name_reporting" {
  value = "${module.source_data_reindex_reporting_pipeline.sierra_topic_name}"
}

output "sierra_items_topic_name_reporting" {
  value = "${module.source_data_reindex_reporting_pipeline.sierra_items_topic_name}"
}