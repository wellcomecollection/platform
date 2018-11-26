# Hybrid store

module "vhs_archive_manifest" {
  source = "modules/vhs"
  name   = "archive-manifests"

  table_read_max_capacity  = 30
  table_write_max_capacity = 30
}

# Security groups

resource "aws_security_group" "service_egress_security_group" {
  name        = "${local.namespace}_service_egress_security_group"
  description = "Allow traffic between services"
  vpc_id      = "${local.vpc_id}"

  egress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"

    cidr_blocks = [
      "0.0.0.0/0",
    ]
  }

  tags {
    Name = "${local.namespace}-egress"
  }
}

resource "aws_security_group" "interservice_security_group" {
  name        = "archive_interservice_security_group"
  description = "Allow traffic between services"
  vpc_id      = "${local.vpc_id}"

  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    self      = true
  }

  tags {
    Name = "${local.namespace}-interservice"
  }
}

data "aws_subnet" "private" {
  count = "${length(local.private_subnets)}"
  id    = "${element(local.private_subnets, count.index)}"
}

resource "aws_security_group" "tcp_access_security_group" {
  name        = "archive_nlb_security_group"
  description = "Allow traffic between load balancer and internet"
  vpc_id      = "${local.vpc_id}"

  ingress {
    protocol    = "tcp"
    from_port   = 9001
    to_port     = 9001
    cidr_blocks = ["${data.aws_subnet.private.*.cidr_block}"]
  }
}

# Security groups - new

resource "aws_security_group" "service_egress" {
  name        = "${local.namespace}_service_egress"
  description = "Allow traffic between services"
  vpc_id      = "${local.vpc_id_new}"

  egress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"

    cidr_blocks = [
      "0.0.0.0/0",
    ]
  }

  tags {
    Name = "${local.namespace}-egress"
  }
}

resource "aws_security_group" "interservice" {
  name        = "archive_interservice"
  description = "Allow traffic between services"
  vpc_id      = "${local.vpc_id_new}"

  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    self      = true
  }

  tags {
    Name = "${local.namespace}-interservice"
  }
}

data "aws_subnet" "private_new" {
  count = "${length(local.private_subnets_new)}"
  id    = "${element(local.private_subnets_new, count.index)}"
}

resource "aws_security_group" "tcp_access" {
  name        = "tcp_access"
  description = "Allow traffic between load balancer and internet"
  vpc_id      = "${local.vpc_id_new}"

  ingress {
    protocol    = "tcp"
    from_port   = 9001
    to_port     = 9001
    cidr_blocks = ["${data.aws_subnet.private_new.*.cidr_block}"]
  }
}

# ECR

data "aws_ecr_repository" "ecr_repository_nginx_services" {
  name = "uk.ac.wellcome/nginx_services"
}

module "ecr_repository_archivist" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "archivist"
}

module "ecr_repository_registrar_async" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "registrar_async"
}

module "ecr_repository_registrar_http" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "registrar_http"
}

module "ecr_repository_progress_async" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "progress_async"
}

module "ecr_repository_progress_http" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "progress_http"
}

module "ecr_repository_notifier" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "notifier"
}

module "ecr_repository_callback_stub_server" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "callback_stub_server"
}

module "ecr_repository_bagger" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "bagger"
}

module "ecr_repository_archive_api" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "archive_api"
}

# S3

data "aws_s3_bucket" "storage_manifests" {
  bucket = "${module.vhs_archive_manifest.bucket_name}"
}

resource "aws_s3_bucket" "archive_storage" {
  bucket = "${local.archive_bucket_name}"
  acl    = "private"

  policy = "${data.aws_iam_policy_document.archive_dlcs_get.json}"
}

resource "aws_s3_bucket" "ingest_storage" {
  bucket = "${local.ingest_bucket_name}"
  acl    = "private"
}

resource "aws_s3_bucket" "storage_static_content" {
  bucket = "${local.storage_static_content_bucket_name}"
  acl    = "private"
}

# IAM

data "aws_iam_policy_document" "archive_dlcs_get" {
  statement {
    effect = "Allow"

    principals {
      type = "AWS"

      identifiers = [
        "arn:aws:iam::653428163053:user/echo-fs",
        "arn:aws:iam::653428163053:user/api",
      ]
    }

    actions = [
      "s3:GetObject",
      "s3:ListBucket",
    ]

    resources = [
      "arn:aws:s3:::${local.archive_bucket_name}",
      "arn:aws:s3:::${local.archive_bucket_name}/*",
    ]
  }
}

data "aws_iam_policy_document" "archive_get" {
  statement {
    actions = [
      "s3:GetObject*",
    ]

    resources = [
      # Allow archivist to read bagger drop bucket
      "arn:aws:s3:::${var.bagger_drop_bucket_name}/*",

      # Allow archivist to read our archive ingest bucket
      "arn:aws:s3:::${local.archive_bucket_name}",

      "arn:aws:s3:::${local.archive_bucket_name}/*",
    ]
  }
}

data "aws_iam_policy_document" "archive_store" {
  statement {
    actions = [
      "s3:PutObject*",
      "s3:GetObject*",
    ]

    resources = [
      "arn:aws:s3:::${local.archive_bucket_name}/*",
    ]
  }
}

data "aws_iam_policy_document" "ingest_get" {
  statement {
    actions = [
      "s3:GetObject*",
      "s3:ListBucket",
    ]

    resources = [
      "arn:aws:s3:::${local.workflow_bucket_name}/*",
      "arn:aws:s3:::${var.bagger_drop_bucket_name}/*",
      "arn:aws:s3:::${local.ingest_bucket_name}/*",
    ]
  }
}

data "aws_iam_policy_document" "bagger_get" {
  statement {
    actions = [
      "s3:GetObject*",
    ]

    resources = [
      "arn:aws:s3:::${var.bagger_mets_bucket_name}",
      "arn:aws:s3:::${var.bagger_mets_bucket_name}/*",
    ]
  }
}

data "aws_iam_policy_document" "bagger_store" {
  statement {
    actions = [
      "s3:PutObject*",
      "s3:GetObject*",
    ]

    resources = [
      "arn:aws:s3:::${local.ingest_bucket_name}/*",
      "arn:aws:s3:::${var.bagger_drop_bucket_name}/*",
      "arn:aws:s3:::${var.bagger_drop_bucket_name_mets_only}/*",
      "arn:aws:s3:::${var.bagger_drop_bucket_name_errors}/*",
    ]
  }
}

data "aws_iam_policy_document" "bagger_get_dlcs" {
  statement {
    actions = [
      "s3:GetObject*",
    ]

    resources = [
      "arn:aws:s3:::${var.bagger_dlcs_source_bucket}",
      "arn:aws:s3:::${var.bagger_dlcs_source_bucket}/*",
    ]
  }
}

data "aws_iam_policy_document" "bagger_get_preservica" {
  statement {
    actions = [
      "s3:GetObject*",
    ]

    resources = [
      "arn:aws:s3:::${var.bagger_current_preservation_bucket}",
      "arn:aws:s3:::${var.bagger_current_preservation_bucket}/*",
    ]
  }
}
