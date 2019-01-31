output "recorder_vhs_bucket_id" {
  value = "${aws_s3_bucket.recorder_vhs.id}"
}

output "recorder_vhs_bucket_arn" {
  value = "${aws_s3_bucket.recorder_vhs.arn}"
}

output "messages_bucket_id" {
  value = "${aws_s3_bucket.messages.id}"
}

output "messages_bucket_arn" {
  value = "${aws_s3_bucket.messages.arn}"
}