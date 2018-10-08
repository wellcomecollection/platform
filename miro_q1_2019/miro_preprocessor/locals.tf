locals {
  lambda_error_alarm_arn            = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
  dlq_alarm_arn                     = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  run_ecs_task_topic_arn            = "${data.terraform_remote_state.shared_infra.run_ecs_task_topic_arn}"
  run_ecs_task_topic_publish_policy = "${data.terraform_remote_state.shared_infra.run_ecs_task_topic_publish_policy}"

  bucket_wellcomecollection_images_arn  = "${data.terraform_remote_state.shared_infra.bucket_wellcomecollection_images_arn}"
  bucket_wellcomecollection_images_name = "${data.terraform_remote_state.shared_infra.bucket_wellcomecollection_images_name}"

  bucket_miro_images_public_arn  = "${data.terraform_remote_state.loris.bucket_wellcomecollection_miro_images_public_arn}"
  bucket_miro_images_public_name = "${data.terraform_remote_state.loris.bucket_wellcomecollection_miro_images_public_id}"

  bucket_miro_images_sync_arn  = "${data.terraform_remote_state.catalogue_api.bucket_miro_images_sync_arn}"
  bucket_miro_images_sync_name = "${data.terraform_remote_state.catalogue_api.bucket_miro_images_sync_id}"

  bucket_miro_data_id  = "${data.terraform_remote_state.catalogue_api.bucket_miro_data_id}"
  bucket_miro_data_arn = "${data.terraform_remote_state.catalogue_api.bucket_miro_data_arn}"

  ecs_services_cluster_name = "${data.terraform_remote_state.catalogue_pipeline.ecs_services_cluster_name}"

  table_miro_data_arn  = "${data.terraform_remote_state.catalogue_pipeline.table_miro_data_arn}"
  table_miro_data_name = "${data.terraform_remote_state.catalogue_pipeline.table_miro_data_name}"
}
