module "network" {
  source     = "git::https://github.com/wellcometrust/terraform.git//network?ref=ecs_v2"
  name       = "${var.namespace}"
  cidr_block = "${var.vpc_cidr_block}"
  az_count   = "2"
}
