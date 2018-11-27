module "grafana" {
  source = "grafana"

  namespace = "${var.namespace}-grafana"

  domain     = "${var.domain}"
  aws_region = "${var.aws_region}"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"

  vpc_id       = "${var.vpc_id}"
  namespace_id = "${var.namespace_id}"

  public_subnets  = "${var.public_subnets}"
  private_subnets = "${var.private_subnets}"

  efs_id                = "${var.efs_id}"
  efs_security_group_id = "${var.efs_security_group_id}"

  key_name           = "${var.key_name}"
  admin_cidr_ingress = "${var.admin_cidr_ingress}"

  grafana_admin_user        = "${var.grafana_admin_user}"
  grafana_anonymous_role    = "${var.grafana_anonymous_role}"
  grafana_admin_password    = "${var.grafana_admin_password}"
  grafana_anonymous_enabled = "${var.grafana_anonymous_enabled}"
}
