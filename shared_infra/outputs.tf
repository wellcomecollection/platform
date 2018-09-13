output "lambda_error_alarm_arn" {
  value = "${module.lambda_error_alarm.arn}"
}

output "ec2_instance_terminating_for_too_long_alarm_arn" {
  value = "${module.ec2_instance_terminating_for_too_long_alarm.arn}"
}

output "dlq_alarm_arn" {
  value = "${module.dlq_alarm.arn}"
}

output "alb_server_error_alarm_arn" {
  value = "${module.alb_server_error_alarm.arn}"
}

output "alb_client_error_alarm_arn" {
  value = "${module.alb_client_error_alarm.arn}"
}

output "terminal_failure_alarm_arn" {
  value = "${module.terminal_failure_alarm.arn}"
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

output "catalogue_private_subnets" {
  value = ["${module.catalogue_vpc.private_subnets}"]
}

output "catalogue_public_subnets" {
  value = ["${module.catalogue_vpc.public_subnets}"]
}

output "catalogue_vpc_private_route_table_ids" {
  value = "${module.catalogue_vpc.private_route_table_ids}"
}

output "catalogue_vpc_id" {
  value = "${module.catalogue_vpc.vpc_id}"
}

output "catalogue_ssh_controlled_ingress_sg" {
  value = "${module.bastion.ssh_controlled_ingress_sg}"
}

output "infra_bucket_arn" {
  value = "${aws_s3_bucket.platform_infra.arn}"
}

output "infra_bucket" {
  value = "${var.infra_bucket}"
}
