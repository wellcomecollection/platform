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
      "desired_count",
    ],
//    Unfortunately, when changing a service, this prevents the creation of the service with
//    InvalidParameterException: Creation of service was not idempotent.
//    Rename the service or delete this line if you get this error
//    Note: DELETING THIS LINE WILL RESULT IN DOWNTIME
//    See https://github.com/hashicorp/terraform/issues/12665 and
//    https://github.com/terraform-providers/terraform-provider-aws/issues/605
    create_before_destroy = true
  }

  depends_on = [
    "aws_iam_role_policy.ecs_service",
    "aws_alb_target_group.ecs_service",
  ]
}
