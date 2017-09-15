data "template_file" "definition" {
  template = "${file("${path.module}/templates/default.json.template")}"

  vars {
    log_group_region = "${var.aws_region}"
    log_group_name   = "${aws_cloudwatch_log_group.task.name}"
    app_uri          = "${var.app_uri}"
    volume_name      = "${var.volume_name}"
    container_path   = "${var.container_path}"
    environment_vars = "[${join(",", var.env_vars)}]"
    cpu              = "${var.cpu}"
    memory           = "${var.memory}"
    name             = "${var.name}"
  }
}
