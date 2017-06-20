resource "aws_cloudwatch_log_group" "task" {
  name = "platform/grafana"
}

data "template_file" "definition" {
  template = "${file("grafana.json.template")}"

  vars {
    log_group_region  = "${var.aws_region}"
    log_group_name    = "${aws_cloudwatch_log_group.task.name}"
    anonymous_enabled = "${var.grafana_anonymous_enabled}"
    anonymous_role    = "${var.grafana_anonymous_role}"
    admin_user        = "${var.grafana_admin_user}"
    admin_password    = "${var.grafana_admin_password}"
  }
}

resource "aws_ecs_task_definition" "task" {
  family                = "grafana_task_definition"
  container_definitions = "${data.template_file.definition.rendered}"
  task_role_arn         = "${module.ecs_grafana_iam.task_role_arn}"

  volume {
    name      = "grafana"
    host_path = "${module.monitoring_userdata.efs_mount_directory}/grafana"
  }
}

resource "aws_ecs_service" "service" {
  name            = "grafana"
  cluster         = "${aws_ecs_cluster.monitoring.id}"
  task_definition = "${aws_ecs_task_definition.task.arn}"
  desired_count   = "1"
  iam_role        = "${aws_iam_role.ecs_service.name}"

  load_balancer {
    target_group_arn = "${aws_alb_target_group.ecs_service.arn}"
    container_name   = "grafana"
    container_port   = "3000"
  }

  lifecycle {
    ignore_changes = [
      "desired_count",
    ]
  }

  depends_on = [
    "aws_iam_role_policy.ecs_service",
    "aws_alb_target_group.ecs_service",
  ]
}

resource "aws_alb_target_group" "ecs_service" {
  name = "grafana"

  port     = 80
  protocol = "HTTP"
  vpc_id   = "${module.vpc_monitoring.vpc_id}"

  health_check {
    path = "/api/health"
  }
}

resource "aws_alb_listener_rule" "rule" {
  listener_arn = "${module.monitoring_alb.listener_arn}"
  priority     = "100"

  action {
    type             = "forward"
    target_group_arn = "${aws_alb_target_group.ecs_service.arn}"
  }

  condition {
    field  = "path-pattern"
    values = ["/*"]
  }
}

resource "aws_iam_role" "ecs_service" {
  name               = "grafana"
  assume_role_policy = "${data.aws_iam_policy_document.assume_ecs_role.json}"
}

data "aws_iam_policy_document" "assume_ecs_role" {
  statement {
    actions = [
      "sts:AssumeRole",
    ]

    principals {
      type        = "Service"
      identifiers = ["ecs.amazonaws.com"]
    }
  }
}

resource "aws_iam_role_policy" "ecs_service" {
  name   = "grafana"
  role   = "${aws_iam_role.ecs_service.name}"
  policy = "${data.aws_iam_policy_document.ecs_service.json}"
}

data "aws_iam_policy_document" "ecs_service" {
  statement {
    actions = [
      "ec2:Describe*",
      "elasticloadbalancing:DeregisterInstancesFromLoadBalancer",
      "elasticloadbalancing:DeregisterTargets",
      "elasticloadbalancing:Describe*",
      "elasticloadbalancing:RegisterInstancesWithLoadBalancer",
      "elasticloadbalancing:RegisterTargets",
    ]

    resources = [
      "*",
    ]
  }
}

module "grafana_efs" {
  name                         = "grafana"
  source                       = "./efs"
  vpc_id                       = "${module.vpc_monitoring.vpc_id}"
  subnets                      = "${module.vpc_monitoring.subnets}"
  efs_access_security_group_id = "${module.monitoring_cluster_asg.instance_sg_id}"
}
