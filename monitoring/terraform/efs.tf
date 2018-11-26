module "grafana_efs" {
  name                         = "grafana"
  source                       = "git::https://github.com/wellcometrust/terraform.git//efs?ref=v1.0.0"
  vpc_id                       = "${local.vpc_id}"
  subnets                      = "${local.private_subnets}"
  efs_access_security_group_id = "${aws_security_group.efs_security_group.id}"
}
