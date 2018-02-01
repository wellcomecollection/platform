module "sierra_store" {
  source = "dynamo_s3_store"

  name        = "sierra_merged-records"
  bucket_name = "${aws_s3_bucket.dynamodb_assets.id}"
}
