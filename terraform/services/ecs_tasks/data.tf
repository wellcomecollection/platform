data "template_file" "definition" {
  template = "${file("${path.module}/templates/${var.template_name}.json.template")}"

  vars {
    app_uri          = "${var.app_uri}"
    nginx_uri        = "${var.nginx_uri}"
    log_group_region = "${var.aws_region}"
    log_group_name   = "${aws_cloudwatch_log_group.task.name}"
    task_name        = "${var.task_name}"
    config_key       = "${var.config_key}"
    infra_bucket     = "${var.infra_bucket}"
  }
}
