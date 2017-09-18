module "xml_to_json_converter" {
  source = "xml_to_json_converter"

  bucket_miro_data_id = "${data.terraform_remote_state.platform.bucket_miro_data_id}"
  release_ids         = "${var.release_ids}"

  s3_read_miro_data_json  = "${data.aws_iam_policy_document.s3_read_miro_data.json}"
  s3_write_miro_data_json = "${data.aws_iam_policy_document.s3_write_miro_data.json}"
}

module "xml_to_json_run_task" {
  source = "xml_to_json_run_task"

  bucket_miro_data_id    = "${data.terraform_remote_state.platform.bucket_miro_data_id}"
  bucket_miro_data_arn   = "${data.terraform_remote_state.platform.bucket_miro_data_arn}"
  lambda_error_alarm_arn = "${data.terraform_remote_state.lambda.lambda_error_alarm_arn}"

  s3_read_miro_data_json = "${data.aws_iam_policy_document.s3_read_miro_data.json}"

  container_name      = "${module.xml_to_json_converter.container_name}"
  topic_arn           = "${data.terraform_remote_state.lambda.run_ecs_task_topic_arn}"
  cluster_name        = "${data.terraform_remote_state.platform.ecs_services_cluster_name}"
  task_definition_arn = "${module.xml_to_json_converter.task_definition_arn}"
}

module "topic_miro_copy_s3_asset" {
  source = "../terraform/sns"
  name   = "miro_copy_s3_asset"
}

module "topic_miro_image_to_dynamo" {
  source = "../terraform/sns"
  name   = "miro_image_to_dynamo"
}

module "miro_image_to_dynamo" {
  source = "miro_image_to_dynamo"

  topic_miro_image_to_dynamo_arn = "${module.topic_miro_image_to_dynamo.arn}"
  miro_data_table_arn            = "${data.terraform_remote_state.platform.table_miro_data_arn}"
  miro_data_table_name           = "${data.terraform_remote_state.platform.table_miro_data_name}"
  lambda_error_alarm_arn         = "${data.terraform_remote_state.lambda.lambda_error_alarm_arn}"
}

module "miro_copy_s3_asset" {
  source = "miro_copy_s3_asset"
  topic_miro_copy_s3_asset_arn = "${module.topic_miro_copy_s3_asset.arn}"
  lambda_error_alarm_arn = "${data.terraform_remote_state.lambda.lambda_error_alarm_arn}"
  topic_miro_image_to_dynamo_arn = "${module.topic_miro_image_to_dynamo.arn}"
  bucket_miro_images_public_arn = "${data.terraform_remote_state.platform.bucket_miro_images_public_arn}"
  bucket_miro_images_public_name = "${data.terraform_remote_state.platform.bucket_miro_images_public_id}"
  bucket_miro_images_sync_arn = "${data.terraform_remote_state.platform.bucket_miro_images_sync_arn}"
  bucket_miro_images_sync_name = "${data.terraform_remote_state.platform.bucket_miro_images_sync_id}"
}
