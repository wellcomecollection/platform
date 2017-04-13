resource "aws_s3_bucket_object" "config_file" {
  bucket  = "${var.infra_bucket}"
  acl     = "private"
  key     = "config/prod/${var.app_name}.conf"
  content = "${data.template_file.config.rendered}"
}
