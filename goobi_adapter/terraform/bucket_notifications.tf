resource "aws_s3_bucket_notification" "bucket_notification" {
  bucket = "${aws_s3_bucket.goobi_adapter.id}"

  topic {
    topic_arn     = "${aws_sns_topic.goobi_notifications_topic.arn}"
    events        = ["s3:ObjectCreated:*"]
    filter_prefix = "goobi_mets/"
    filter_suffix = ".xml"
  }
}
