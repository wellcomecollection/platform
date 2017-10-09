output "ec2_terminating_topic_arn" {
  value = "${module.ec2_terminating_topic.arn}"
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

output "ec2_terminating_topic_publish_policy" {
  value = "${module.ec2_terminating_topic.publish_policy}"
}

output "bucket_infra_arn" {
  value = "${aws_s3_bucket.infra.arn}"
}

output "bucket_infra_id" {
  value = "${aws_s3_bucket.infra.id}"
}

output "bucket_miro_data_id" {
  value = "${aws_s3_bucket.miro-data.id}"
}

output "bucket_miro_data_arn" {
  value = "${aws_s3_bucket.miro-data.arn}"
}

output "bucket_miro_images_sync_arn" {
  value = "${aws_s3_bucket.miro-images-sync.arn}"
}

output "bucket_miro_images_sync_id" {
  value = "${aws_s3_bucket.miro-images-sync.id}"
}

# This should be removed when they are superseded by bucket_wellcomecollectio_miro_images_public_* below
output "bucket_miro_images_public_arn" {
  value = "${aws_s3_bucket.miro_images_public.arn}"
}

# Outputs required for Loris

output "cloudfront_logs_domain_name" {
  value = "${aws_s3_bucket.cloudfront-logs.bucket_domain_name}"
}

output "vpc_api_id" {
  value = "${module.vpc_api.vpc_id}"
}

output "vpc_api_subnets" {
  value = "${module.vpc_api.subnets}"
}

output "ecs_ami_id" {
  value = "${data.aws_ami.stable_coreos.id}"
}

output "bucket_alb_logs_id" {
  value = "${aws_s3_bucket.alb-logs.id}"
}
