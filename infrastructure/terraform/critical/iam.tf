# elasticcloud

module "elasticcloud-catalogue" {
  source = "../modules/elasticcloud"

  bucket_name = "${aws_s3_bucket.elasticsearch-snapshots-catalogue.id}"
  namespace   = "catalogue"
  pgp_pub_key = "${local.wellcomedigitalplatform_pgp_pub_key}"

  principals = [
    "${local.aws_platform_principal}"
  ]
}