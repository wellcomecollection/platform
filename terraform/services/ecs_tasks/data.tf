data "template_file" "definition" {
  template = "${file("${path.module}/templates/${var.template_name}.json.template")}"

  vars {
    log_group_region     = "${var.aws_region}"
    log_group_name       = "${aws_cloudwatch_log_group.task.name}"
    nginx_log_group_name = "${aws_cloudwatch_log_group.nginx_task.name}"

    app_uri      = "${var.app_uri}"
    nginx_uri    = "${var.nginx_uri}"
    config_key   = "${var.config_key}"
    infra_bucket = "${var.infra_bucket}"

    docker_image     = "${var.docker_image}"
    name             = "${var.task_name}"
    container_port   = "${var.container_port}"
    volume_name      = "${var.volume_name}"
    container_path   = "${var.container_path}"
    environment_vars = "${var.environment_vars}"
  }
}
