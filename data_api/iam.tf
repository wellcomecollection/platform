module "ecs_elasticdump_iam" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs_iam?ref=v1.0.0"
  name   = "elasticdump"
}
