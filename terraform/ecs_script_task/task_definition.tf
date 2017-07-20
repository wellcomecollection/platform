resource "aws_ecs_task_definition" "task" {
  family                = "${var.task_name}_task_definition"
  container_definitions = "${data.template_file.definition.rendered}"
  task_role_arn         = "${var.task_role_arn}"

  volume {
    name      = "${var.volume_name}"
    host_path = "${var.volume_host_path}"
  }
}
