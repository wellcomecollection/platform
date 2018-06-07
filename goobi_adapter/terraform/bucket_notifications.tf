//resource "aws_s3_bucket_notification" "bucket_notification" {
//  bucket = "${aws_s3_bucket.goobi_adapter.id}"
//
//  topic {
//    topic_arn     = "${goobi_notifications_topic_arn}"
//    events        = ["s3:ObjectCreated:*"]
//    filter_suffix = ".xml"
//  }
//}
