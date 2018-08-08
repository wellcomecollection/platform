module "transformer_dlq_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "${var.namespace}_transformer_dlq_alarm"
}

module "es_ingest_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "${var.namespace}_es_ingest"
}

module "transformed_works_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "${var.namespace}_transformed_works"
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
