resource "aws_route53_zone" "wecodev" {
  name = "wecodev.com"
}

resource "aws_route53_record" "notebook" {
  zone_id = "${aws_route53_zone.wecodev.zone_id}"
  name    = "notebook.wecodev.com"
  type    = "A"
  ttl     = "300"
  records = ["${module.pepperami.public_ip}"]
}

resource "aws_route53_record" "notebook-ns" {
  zone_id = "${aws_route53_zone.wecodev.zone_id}"
  name    = "notebook.wecodev.com"
  type    = "NS"
  ttl     = "30"

  records = [
    "${aws_route53_zone.wecodev.name_servers.0}",
    "${aws_route53_zone.wecodev.name_servers.1}",
    "${aws_route53_zone.wecodev.name_servers.2}",
    "${aws_route53_zone.wecodev.name_servers.3}",
  ]
}

module "pepperami" {
  source = "../../modules/dlami-pepperami"

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