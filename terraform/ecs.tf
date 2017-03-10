resource "aws_ecs_cluster" "main" {
  name = "platform_cluster"
}

resource "aws_ecs_cluster" "tools" {
  name = "tools_cluster"
}

module "api_task" {
  source        = "./ecs_tasks"
  task_name     = "api"
  image_uri     = "${aws_ecr_repository.api.repository_url}:${var.release_id}"
  task_role_arn = "${module.ecs_main_iam.task_role_arn}"
}

module "jenkins_task" {
  source           = "./ecs_tasks"
  task_name        = "jenkins"
  task_role_arn    = "${module.ecs_tools_iam.task_role_arn}"
  volume_name      = "jenkins-home"
  volume_host_path = "/mnt/efs"
}
