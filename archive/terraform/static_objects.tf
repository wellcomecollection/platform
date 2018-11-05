resource "aws_s3_bucket_object" "context" {
  bucket = "${local.storage_static_content_bucket_name}"
  key    = "context.json"
  source = "../resources/context.json"
  etag   = "${md5(file("../resources/context.json"))}"
}
