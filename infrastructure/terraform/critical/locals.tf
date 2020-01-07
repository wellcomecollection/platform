locals {
  account_id    = "${data.aws_caller_identity.current.account_id}"
  aws_platform_principal = "arn:aws:iam::${local.account_id}:root"
  wellcomedigitalplatform_pgp_pub_key = "${data.template_file.pgp_pub_key.rendered}"
}

data "template_file" "pgp_pub_key" {
  template = "${file("${path.module}/wellcomedigitalplatform.pub")}"
}