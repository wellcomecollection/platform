resource "aws_iam_role_policy" "lambda_trigger_bag_ingest_kms" {
  name   = "lambda_trigger_bag_ingest_use_encryption_key"
  role   = "${module.lambda_trigger_bag_ingest.role_name}"
  policy = "${module.kms_key.use_encryption_key_policy}"
}
