output "snapshots_bucket_arn" {
  value = "${aws_s3_bucket.public_data.arn}"
}
