module "sierra_adapter_iam" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs_iam?ref=v1.0.0"
  name   = "sierra_adapter"
}
