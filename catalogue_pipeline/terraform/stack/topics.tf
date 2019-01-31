module "transformed_miro_works_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "${var.namespace}_transformed_miro_works"
}

module "transformed_sierra_works_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "${var.namespace}_transformed_sierra_works"
}

module "transformer_dlq_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "${var.namespace}_transformer_dlq_alarm"
}

module "es_ingest_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "${var.namespace}_es_ingest"
}

module "recorded_works_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "${var.namespace}_recorded_works"
}

module "merged_works_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "${var.namespace}_merged_works"
}

module "matched_works_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "${var.namespace}_matched_works"
}
