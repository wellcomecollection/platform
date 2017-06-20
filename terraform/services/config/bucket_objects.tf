resource "aws_s3_bucket_object" "config_file" {
  bucket  = "${var.infra_bucket}"
  acl     = "private"
  key     = "${var.config_key}"
  content = "${data.template_file.config.rendered}"
  count = "${var.is_config_managed}"
}
