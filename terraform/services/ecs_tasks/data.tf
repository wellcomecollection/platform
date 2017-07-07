data "template_file" "definition" {
  template = "${file("${path.module}/templates/${var.template_name}.json.template")}"

  vars {
    log_group_region = "${var.aws_region}"
    log_group_name   = "${aws_cloudwatch_log_group.task.name}"

    app_uri      = "${var.app_uri}"
    nginx_uri    = "${var.nginx_uri}"
    primary_container_port   = "${var.primary_container_port}"
    secondary_container_port   = "${var.secondary_container_port}"
    volume_name      = "${var.volume_name}"
    container_path   = "${var.container_path}"
    environment_vars = "${var.environment_vars}"
  }
}
