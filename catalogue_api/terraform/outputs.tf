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
