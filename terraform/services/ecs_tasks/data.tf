data "template_file" "definition" {
  template = "${file("${path.module}/templates/${var.template_name}.json.template")}"

  vars {
    image_uri        = "${var.image_uri}"
    log_group_region = "${var.aws_region}"
    log_group_name   = "${aws_cloudwatch_log_group.task.name}"
    task_name        = "${var.task_name}"
  }
}
