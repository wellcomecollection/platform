resource "aws_s3_bucket_object" "context" {
  bucket  = "${var.storage_static_content_bucket_name}"
  key     = "static/context.json"
  content = "${file("${path.module}/context.json")}"
  etag    = "${md5(file("${path.module}/context.json"))}"
}
