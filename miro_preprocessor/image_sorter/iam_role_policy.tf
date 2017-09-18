resource "aws_iam_role_policy" "allow_s3_reads" {
  role   = "${module.image_sorter_lambda.role_name}"
  policy = "${data.aws_iam_policy_document.s3_read_miro_json.json}"
}

resource "aws_iam_role_policy" "allow_sns_publish_cold_store" {
  role   = "${module.image_sorter_lambda.role_name}"
  policy = "${var.topic_cold_store_publish_policy}"
}

resource "aws_iam_role_policy" "allow_sns_publish_tandem_vault" {
  role   = "${module.image_sorter_lambda.role_name}"
  policy = "${var.topic_tandem_vault_publish_policy}"
}

resource "aws_iam_role_policy" "allow_sns_publish_catalogue_api" {
  role   = "${module.image_sorter_lambda.role_name}"
  policy = "${var.topic_catalogue_api_publish_policy}"
}
