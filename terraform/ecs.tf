resource "aws_ecs_cluster" "main" {
  name = "platform_cluster"
}

resource "aws_ecs_cluster" "tools" {
  name = "tools_cluster"
}

data "template_file" "api_task_definition" {
  template = "${file("${path.module}/tasks/api.json")}"

  vars {
    container_name   = "platform_api"
    image_uri        = "${aws_ecr_repository.api.repository_url}:${var.release_id}"
    log_group_region = "${var.aws_region}"
    log_group_name   = "${aws_cloudwatch_log_group.app.name}"
  }
}

resource "aws_ecs_task_definition" "platform_api" {
  family                = "platform_task_definition"
  container_definitions = "${data.template_file.api_task_definition.rendered}"
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
  task_role_arn         = "${module.ecs_tools_iam.task_role_arn}"

  volume {
    name      = "jenkins-home"
    host_path = "/mnt/efs"
  }
}
