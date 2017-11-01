resource "aws_iam_role_policy" "allow_uploader_s3_get" {
  role   = "${module.ecs_tandem_vault_uploader_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_get.json}"
}

resource "aws_iam_role_policy" "allow_uploader_sqs_read" {
  role   = "${module.ecs_tandem_vault_uploader_iam.task_role_name}"
  policy = "${module.upload_image_queue.read_policy}"
}

resource "aws_iam_role_policy" "allow_uploader_sns_publish" {
  role   = "${module.ecs_tandem_vault_uploader_iam.task_role_name}"
  policy = "${module.enrich_image_topic.publish_policy}"
}

resource "aws_iam_role_policy" "allow_enrichment_sqs_read" {
  role   = "${module.ecs_tandem_vault_enrichment_iam.task_role_name}"
  policy = "${module.enrich_image_queue.read_policy}"
}

resource "aws_iam_role_policy" "allow_enrichment_s3_get" {
  role   = "${module.ecs_tandem_vault_enrichment_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_get.json}"
}

data "aws_iam_policy_document" "allow_s3_get" {
  statement {
    actions = [
      "s3:GetObject",
    ]

    resources = [
      "${var.bucket_source_asset_arn}/*",
    ]
  }
}

module "upload_image_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v1.0.0"
  queue_name  = "upload_image_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${var.topic_miro_image_to_tandem_vault_name}"]

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

module "enrich_image_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v1.0.0"
  queue_name  = "enrich_image_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${module.enrich_image_topic.arn}"]

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

module "enrich_image_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "enrich_image_topic"
}

module "tandem_vault_uploader" {
  source        = "git::https://github.com/wellcometrust/terraform.git//ecs_script_task?ref=v1.0.0"
  task_name     = "tandem_vault_uploader"
  app_uri       = "${module.ecr_repository_tandem_vault_uploader.repository_url}:${var.release_ids["tandem_vault_uploader"]}"
  task_role_arn = "${module.ecs_tandem_vault_uploader_iam.task_role_arn}"

  env_vars = [
    "{\"name\": \"QUEUE_URL\", \"value\": \"${module.enrich_image_queue.id}\"}",
    "{\"name\": \"TOPIC_ARN\", \"value\": \"${module.enrich_image_topic.arn}\"}",
    "{\"name\": \"TANDEM_VAULT_API_KEY\", \"value\": \"${var.tandem_vault_api_key}\"}",
    "{\"name\": \"TANDEM_VAULT_API_URL\", \"value\": \"${var.tandem_vault_api_url}\"}",
    "{\"name\": \"IMAGE_SRC_BUCKET\", \"value\": \"${var.bucket_source_asset_name}\"}",
  ]
}

module "ecs_tandem_vault_uploader_iam" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs_iam?ref=v1.0.0"
  name   = "tandem_vault_uploader"
}

module "ecr_repository_tandem_vault_uploader" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "tandem_vault_uploader"
}

module "tandem_vault_enrichment" {
  source        = "git::https://github.com/wellcometrust/terraform.git//ecs_script_task?ref=v1.0.0"
  task_name     = "tandem_vault_enrichment"
  app_uri       = "${module.ecr_repository_tandem_vault_enrichment.repository_url}:${var.release_ids["tandem_vault_enrichment"]}"
  task_role_arn = "${module.ecs_tandem_vault_enrichment_iam.task_role_arn}"

  env_vars = [
    "{\"name\": \"QUEUE_URL\", \"value\": \"${module.enrich_image_queue.id}\"}",
    "{\"name\": \"TANDEM_VAULT_API_KEY\", \"value\": \"${var.tandem_vault_api_key}\"}",
    "{\"name\": \"TANDEM_VAULT_API_URL\", \"value\": \"${var.tandem_vault_api_url}\"}",
  ]
}

module "ecs_tandem_vault_enrichment_iam" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs_iam?ref=v1.0.0"
  name   = "tandem_vault_enrichment"
}

module "ecr_repository_tandem_vault_enrichment" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "tandem_vault_enrichment"
}
