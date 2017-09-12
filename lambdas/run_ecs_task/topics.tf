module "run_ecs_task_topic" {
  source = "../terraform/sns"
  name   = "run_ecs_task"
}
