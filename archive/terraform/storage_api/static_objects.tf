resource "aws_s3_bucket_object" "context" {
  bucket = "${var.storage_static_content_bucket_name}"
  key    = "/static/context.json"
  source = "context.json"
  etag   = "${md5(file("context.json"))}"
}
