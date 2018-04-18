module "snapshot_scheduler" {
  source = "snapshot_scheduler"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
  infra_bucket           = "${var.infra_bucket}"

  es_index_v1 = "${local.es_index_v1}"
  es_index_v2 = "${local.es_index_v2}"

  private_bucket_name = "${aws_s3_bucket.private_data.id}"
  public_bucket_name  = "${aws_s3_bucket.public_data.id}"

  public_object_key_v1 = "catalogue/v1/works.json.gz"
  public_object_key_v2 = "catalogue/v2/works.json.gz"
}
