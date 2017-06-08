resource "aws_ecs_service" "service" {
  name            = "${var.service_name}"
  cluster         = "${var.cluster_id}"
  task_definition = "${var.task_definition_arn}"
  desired_count   = "${var.desired_count}"
  iam_role        = "${aws_iam_role.ecs_service.name}"

  load_balancer {
    target_group_arn = "${aws_alb_target_group.ecs_service.arn}"
    container_name   = "${var.container_name}"
    container_port   = "${var.container_port}"
  }

  lifecycle {
    ignore_changes = [
      "desired_count"
    ]
  }

  depends_on = [
    "aws_iam_role_policy.ecs_service",
    "aws_alb_target_group.ecs_service",
  ]
}
