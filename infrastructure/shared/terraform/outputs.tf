output "lambda_error_alarm_arn" {
  value = "${module.lambda_error_alarm.arn}"
}

output "dlq_alarm_arn" {
  value = "${module.dlq_alarm.arn}"
}

output "gateway_server_error_alarm_arn" {
  value = "${module.gateway_server_error_alarm.arn}"
}

output "ec2_terminating_topic_arn" {
  value = "${module.ec2_terminating_topic.arn}"
}

output "ec2_terminating_topic_publish_policy" {
  value = "${module.ec2_terminating_topic.publish_policy}"
}

output "bucket_alb_logs_id" {
  value = "${aws_s3_bucket.alb_logs.id}"
}

output "terraform_apply_topic_name" {
  value = "${module.terraform_apply_topic.name}"
}

output "cloudfront_logs_bucket_domain_name" {
  value = "${aws_s3_bucket.cloudfront_logs.bucket_domain_name}"
}

output "infra_bucket_arn" {
  value = "${aws_s3_bucket.platform_infra.arn}"
}

output "infra_bucket" {
  value = "${var.infra_bucket}"
}

# Reindexing topics

## Reporting - miro

output "reporting_miro_reindex_topic_arn" {
  value = "${module.reporting_miro_reindex_topic.arn}"
}

output "reporting_miro_reindex_topic_name" {
  value = "${module.reporting_miro_reindex_topic.name}"
}

## Reporting - miro inventory

output "reporting_miro_inventory_reindex_topic_arn" {
  value = "${module.reporting_miro_inventory_reindex_topic.arn}"
}

output "reporting_miro_inventory_reindex_topic_name" {
  value = "${module.reporting_miro_inventory_reindex_topic.name}"
}

## Reporting - sierra

output "reporting_sierra_reindex_topic_arn" {
  value = "${module.reporting_sierra_reindex_topic.arn}"
}

output "reporting_sierra_reindex_topic_name" {
  value = "${module.reporting_sierra_reindex_topic.name}"
}

## Catalogue - miro

output "catalogue_miro_reindex_topic_arn" {
  value = "${module.catalogue_miro_reindex_topic.arn}"
}

output "catalogue_miro_reindex_topic_name" {
  value = "${module.catalogue_miro_reindex_topic.name}"
}

## Catalogue - sierra

output "catalogue_sierra_reindex_topic_arn" {
  value = "${module.catalogue_sierra_reindex_topic.arn}"
}

output "catalogue_sierra_reindex_topic_name" {
  value = "${module.catalogue_sierra_reindex_topic.name}"
}

## Catalogue - sierra items

output "catalogue_sierra_items_reindex_topic_arn" {
  value = "${module.catalogue_sierra_items_reindex_topic.arn}"
}

output "catalogue_sierra_items_reindex_topic_name" {
  value = "${module.catalogue_sierra_items_reindex_topic.name}"
}

# Catalogue VPC

output "catalogue_vpc_delta_private_subnets" {
  value = ["${module.catalogue_vpc_delta.private_subnets}"]
}

output "catalogue_vpc_delta_public_subnets" {
  value = ["${module.catalogue_vpc_delta.public_subnets}"]
}

output "catalogue_vpc_delta_id" {
  value = "${module.catalogue_vpc_delta.vpc_id}"
}

# Storage VPC

output "storage_vpc_delta_private_subnets" {
  value = ["${module.storage_vpc_delta.private_subnets}"]
}

output "storage_vpc_delta_public_subnets" {
  value = ["${module.storage_vpc_delta.public_subnets}"]
}

output "storage_vpc_delta_id" {
  value = "${module.storage_vpc_delta.vpc_id}"
}

output "storage_cidr_block_vpc" {
  value = "${local.storage_cidr_block_vpc}"
}

# Monitoring VPC

output "monitoring_vpc_delta_private_subnets" {
  value = ["${module.monitoring_vpc_delta.private_subnets}"]
}

output "monitoring_vpc_delta_public_subnets" {
  value = ["${module.monitoring_vpc_delta.public_subnets}"]
}

output "monitoring_vpc_delta_id" {
  value = "${module.monitoring_vpc_delta.vpc_id}"
}

# Data science VPC

output "datascience_vpc_delta_private_subnets" {
  value = ["${module.datascience_vpc_delta.private_subnets}"]
}

output "datascience_vpc_delta_public_subnets" {
  value = ["${module.datascience_vpc_delta.public_subnets}"]
}

output "datascience_vpc_delta_id" {
  value = "${module.datascience_vpc_delta.vpc_id}"
}
