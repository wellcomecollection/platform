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
