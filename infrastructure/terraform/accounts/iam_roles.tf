data "aws_iam_policy_document" "assume_role_policy" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      identifiers = [local.aws_principal]
      type        = "AWS"
    }
  }
}

# Workflow support role

module "workflow_support_role" {
  source = "./modules/assumable_role/aws"

  providers = {
    aws = aws.workflow
  }

  name = "workflow-support"

  principals = [local.aws_principal]
}

resource "aws_iam_role_policy" "workflow_support" {
  provider = aws.workflow

  role   = module.workflow_support_role.name
  policy = data.aws_iam_policy_document.workflow_support.json
}

data "aws_iam_policy_document" "workflow_support" {
  provider = aws.workflow

  statement {
    actions = [
      "s3:ListBucket",
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject",
      "s3:RestoreObject",
    ]

    resources = [
      "arn:aws:s3:::wellcomecollection-archivematica-transfer-source",
      "arn:aws:s3:::wellcomecollection-archivematica-transfer-source/*",
      "arn:aws:s3:::wellcomecollection-archivematica-staging-transfer-source",
      "arn:aws:s3:::wellcomecollection-archivematica-staging-transfer-source/*",

      "arn:aws:s3:::wellcomecollection-client-transfer",
      "arn:aws:s3:::wellcomecollection-client-transfer/*",
      "arn:aws:s3:::wellcomecollection-workflow-upload",
      "arn:aws:s3:::wellcomecollection-workflow-upload/*",
      "arn:aws:s3:::wellcomecollection-editorial-photography",
      "arn:aws:s3:::wellcomecollection-editorial-photography/*",
    ]
  }
}

# s3_scala_releases
resource "aws_iam_role" "s3_scala_releases_read" {
  name               = "travis_machine_s3_scala_releases"
  assume_role_policy = data.aws_iam_policy_document.assume_role_policy.json
}

resource "aws_iam_role_policy" "s3_scala_releases_read" {
  policy = data.aws_iam_policy_document.s3_scala_releases_read.json
  role   = aws_iam_role.s3_scala_releases_read.name
}

data "aws_iam_policy_document" "s3_scala_releases_read" {
  statement {
    actions = [
      "s3:Get*",
      "s3:List*",
    ]

    resources = [
      "arn:aws:s3:::releases.mvn-repo.wellcomecollection.org/*",
    ]
  }
}

module "s3_releases_scala_sierra_client" {
  source = "./modules/s3_scala_releases"
  name   = "scala-sierra-client"
}

module "s3_releases_scala_fixtures" {
  source = "./modules/s3_scala_releases"
  name   = "fixtures"
}

module "s3_releases_scala_json" {
  source = "./modules/s3_scala_releases"
  name   = "json"
}

module "s3_releases_scala_monitoring" {
  source = "./modules/s3_scala_releases"
  name   = "monitoring"
}

module "s3_releases_scala_storage" {
  source = "./modules/s3_scala_releases"
  name   = "storage"
}

module "s3_releases_scala_messaging" {
  source = "./modules/s3_scala_releases"
  name   = "messaging"
}

module "s3_releases_scala_typesafe" {
  source = "./modules/s3_scala_releases"
  name   = "typesafe-app"
}

module "s3_releases_scala_catalogue_client" {
  source = "./modules/s3_scala_releases"
  name   = "scala-catalogue-client"
}
