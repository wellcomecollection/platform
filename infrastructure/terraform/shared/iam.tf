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

# cloudhealth

module "cloudhealth-catalogue" {
  source = "../modules/cloudhealth"

  providers = {
    aws = "aws.catalogue"
  }
}

module "cloudhealth-storage" {
  source = "../modules/cloudhealth"

  providers = {
    aws = "aws.storage"
  }
}

module "cloudhealth-platform" {
  source = "../modules/cloudhealth"

  providers = {
    aws = "aws.platform"
  }
}

module "cloudhealth-datascience" {
  source = "../modules/cloudhealth"

  providers = {
    aws = "aws.datascience"
  }
}

module "cloudhealth-experience" {
  source = "../modules/cloudhealth"

  providers = {
    aws = "aws.experience"
  }
}

module "cloudhealth-digitisation" {
  source = "../modules/cloudhealth"

  providers = {
    aws = "aws.digitisation"
  }
}

module "cloudhealth-workflow" {
  source = "../modules/cloudhealth"

  providers = {
    aws = "aws.workflow"
  }
}

module "cloudhealth-reporting" {
  source = "../modules/cloudhealth"

  providers = {
    aws = "aws.reporting"
  }
}