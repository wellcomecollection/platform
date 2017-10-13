module "xml_to_json_converter" {
  source = "xml_to_json_converter"

  bucket_miro_data_id = "${local.bucket_miro_data_id}"
  release_ids         = "${var.release_ids}"

  s3_read_miro_data_json  = "${data.aws_iam_policy_document.s3_read_miro_data.json}"
  s3_write_miro_data_json = "${data.aws_iam_policy_document.s3_write_miro_data.json}"
}

module "xml_to_json_run_task" {
  source = "xml_to_json_run_task"

  bucket_miro_data_id    = "${local.bucket_miro_data_id}"
  bucket_miro_data_arn   = "${local.bucket_miro_data_arn}"
  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"

  s3_read_miro_data_json = "${data.aws_iam_policy_document.s3_read_miro_data.json}"

  container_name      = "${module.xml_to_json_converter.container_name}"
  topic_arn           = "${local.run_ecs_task_topic_arn}"
  cluster_name        = "${local.ecs_services_cluster_name}"
  task_definition_arn = "${module.xml_to_json_converter.task_definition_arn}"

  run_ecs_task_topic_publish_policy = "${local.run_ecs_task_topic_publish_policy}"
}

module "miro_image_to_dynamo" {
  source = "miro_image_to_dynamo"

  topic_miro_image_to_dynamo_arn = "${module.topic_miro_image_to_dynamo.arn}"
  miro_data_table_arn            = "${local.table_miro_data_arn}"
  miro_data_table_name           = "${local.table_miro_data_name}"
  lambda_error_alarm_arn         = "${local.lambda_error_alarm_arn}"
}

module "miro_image_sorter" {
  source = "image_sorter"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"

  s3_miro_data_id  = "${local.bucket_miro_data_id}"
  s3_miro_data_arn = "${local.bucket_miro_data_arn}"

  s3_exceptions_key = "source/exceptions.csv"

  topic_cold_store_arn                 = "${module.cold_store_topic.arn}"
  topic_cold_store_publish_policy      = "${module.cold_store_topic.publish_policy}"
  topic_tandem_vault_arn               = "${module.tandem_vault_topic.arn}"
  topic_tandem_vault_publish_policy    = "${module.tandem_vault_topic.publish_policy}"
  topic_catalogue_api_arn              = "${module.catalogue_api_topic.arn}"
  topic_catalogue_api_publish_policy   = "${module.catalogue_api_topic.publish_policy}"
  topic_digital_library_arn            = "${module.digital_library_topic.arn}"
  topic_digital_library_publish_policy = "${module.digital_library_topic.publish_policy}"
  topic_none_arn                       = "${module.none_topic.arn}"
  topic_none_publish_policy            = "${module.none_topic.publish_policy}"
}

module "miro_copy_catalogue_derivative" {
  source                        = "miro_copy_s3_asset"
  topic_miro_copy_s3_asset_arn  = "${module.catalogue_api_topic.arn}"
  topic_forward_sns_message_arn = "${module.topic_miro_image_to_dynamo.arn}"

  lambda_error_alarm_arn       = "${local.lambda_error_alarm_arn}"
  bucket_destination_asset_arn = "${local.bucket_miro_images_public_arn}"
  bucket_destination_name      = "${local.bucket_miro_images_public_name}"
  bucket_source_asset_arn      = "${local.bucket_miro_images_sync_arn}"
  bucket_source_asset_name     = "${local.bucket_miro_images_sync_name}"

  lambda_description     = "Copy catalogue miro derivatives to Loris s3 bucket"
  lambda_name            = "miro_copy_catalogue_derivative"
  is_master_asset = "false"
  destination_key_prefix = ""
}

module "miro_copy_catalogue_master" {
  source                        = "miro_copy_s3_asset"
  topic_miro_copy_s3_asset_arn  = "${module.catalogue_api_topic.arn}"

  lambda_error_alarm_arn       = "${local.lambda_error_alarm_arn}"
  bucket_destination_asset_arn = "${aws_s3_bucket.wellcomecollection-images.arn}"
  bucket_destination_name      = "${aws_s3_bucket.wellcomecollection-images.id}"
  bucket_source_asset_arn      = "${local.bucket_miro_images_sync_arn}"
  bucket_source_asset_name     = "${local.bucket_miro_images_sync_name}"

  destination_key_prefix = "library/"
  lambda_description     = "Copy catalogue miro master assets to private s3 bucket"
  lambda_name            = "miro_copy_catalogue_master"
  is_master_asset = "false"
}

resource "aws_iam_role_policy" "miro_copy_s3_derivative_asset_sns_publish" {
  name   = "miro_copy_s3_derivative_asset_sns_publish_policy"
  role   = "${module.miro_copy_catalogue_derivative.role_name}"
  policy = "${module.topic_miro_image_to_dynamo.publish_policy}"
}

resource "aws_s3_bucket_notification" "bucket_notification" {
  bucket = "${local.bucket_miro_data_id}"

  lambda_function {
    lambda_function_arn = "${module.xml_to_json_run_task.arn}"
    events              = ["s3:ObjectCreated:*"]
    filter_prefix       = "source/"
    filter_suffix       = ".xml"
  }

  lambda_function {
    lambda_function_arn = "${module.miro_image_sorter.arn}"
    events              = ["s3:ObjectCreated:*"]
    filter_prefix       = "json/"
    filter_suffix       = ".json"
  }
}
