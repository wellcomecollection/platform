resource "aws_route53_zone" "wecodev" {
  name = "wecodev.com"
}

resource "aws_route53_record" "notebook" {
  zone_id = "${aws_route53_zone.wecodev.zone_id}"
  name    = "notebook.wecodev.com"
  type    = "CNAME"
  ttl     = "300"
  records = ["${module.dlami.public_dns}"]
}

module "dlami" {
  source = "../../modules/dlami"

  key_name    = "${var.key_name}"
  bucket_name = "${var.s3_bucket_name}"

  ebs_volume_size = "${var.ebs_volume_size}"
  vpc_id      = "${var.vpc_id}"

  instance_type = "${var.instance_type}"
  name   = "jupyter-${var.namespace}"

  custom_security_groups = [
    "${var.efs_security_group_id}",
  ]

  default_environment = "pytorch_p36"

  controlled_access_cidr_ingress = ["${var.controlled_access_cidr_ingress}"]

  efs_mount_id = "${var.efs_id}"
}