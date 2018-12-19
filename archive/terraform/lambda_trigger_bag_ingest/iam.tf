module "lambda_trigger_bag_ingest_iam" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/modules/iam?ref=v17.1.0"
  name   = "${var.name}"
}
