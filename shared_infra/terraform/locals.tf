locals {
  miro_transformer_topic_arn = "${data.terraform_remote_state.catalogue_pipeline.miro_transformer_topic_arn}"
  miro_table_stream_arn      = "${data.terraform_remote_state.catalogue_pipeline.miro_table_stream_arn}"

  miro_transformer_topic_publish_policy = "${data.terraform_remote_state.catalogue_pipeline.miro_transformer_topic_publish_policy}"
  calm_transformer_topic_publish_policy = "${data.terraform_remote_state.catalogue_pipeline.calm_transformer_topic_publish_policy}"

  dynamodb_table_reindex_tracker_stream_arn = "${data.terraform_remote_state.catalogue_pipeline.dynamodb_table_reindex_tracker_stream_arn}"
  ecs_services_cluster_name                 = "${data.terraform_remote_state.catalogue_pipeline.ecs_services_cluster_name}"
  dynamodb_table_miro_table_name            = "${data.terraform_remote_state.catalogue_pipeline.dynamodb_table_miro_table_name}"
}
