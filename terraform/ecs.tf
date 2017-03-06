resource "aws_ecs_cluster" "main" {
  name = "platform_cluster"
}

resource "aws_ecs_cluster" "tools" {
  name = "tools_cluster"
}

resource "aws_ecs_service" "platform" {
  name            = "platform_ecs_service"
  cluster         = "${aws_ecs_cluster.main.id}"
  task_definition = "${aws_ecs_task_definition.platform_api.arn}"
  desired_count   = 1
  iam_role        = "${aws_iam_role.ecs_service.name}"

  load_balancer {
    target_group_arn = "${aws_alb_target_group.platform.id}"
    container_name   = "platform_api"
    container_port   = "8888"
  }

  depends_on = [
    "aws_iam_role_policy.ecs_service",
    "aws_alb_listener.platform_api_listener",
  ]
}

data "template_file" "platform_task_definition" {
  template = "${file("${path.module}/tasks/platform-api.json")}"

  vars {
    container_name   = "platform_api"
    image_uri        = "${aws_ecr_repository.api.repository_url}:${var.release_id}"
    log_group_region = "${var.aws_region}"
    log_group_name   = "${aws_cloudwatch_log_group.app.name}"
  }
}

resource "aws_ecs_task_definition" "platform_api" {
  family                = "platform_task_definition"
  container_definitions = "${data.template_file.platform_task_definition.rendered}"
}

resource "aws_ecs_service" "jenkins" {
  name            = "jenkins_ecs_service"
  cluster         = "${aws_ecs_cluster.tools.id}"
  task_definition = "${aws_ecs_task_definition.jenkins.arn}"
  desired_count   = 1
  iam_role        = "${aws_iam_role.ecs_service.name}"

  load_balancer {
    target_group_arn = "${aws_alb_target_group.jenkins.id}"
    container_name   = "jenkins"
    container_port   = "8080"
  }

  depends_on = [
    "aws_iam_role_policy.ecs_service",
    "aws_alb_listener.jenkins_listener",
  ]
}

data "template_file" "jenkins_task_definition" {
  template = "${file("${path.module}/tasks/jenkins.json")}"

  vars {
    container_name   = "jenkins"
    log_group_region = "${var.aws_region}"
    log_group_name   = "${aws_cloudwatch_log_group.app_jenkins.name}"
  }
}

resource "aws_ecs_task_definition" "jenkins" {
  family                = "jenkins_task_definition"
  container_definitions = "${data.template_file.jenkins_task_definition.rendered}"
  task_role_arn         = "${aws_iam_role.ecs_jenkins_task.arn}"

  volume {
    name      = "jenkins-home"
    host_path = "/mnt/efs"
  }
}
