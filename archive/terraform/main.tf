resource "aws_ecs_cluster" "cluster" {
  name = "${local.namespace}"
}

resource "aws_service_discovery_private_dns_namespace" "namespace" {
  name = "${local.namespace}"
  vpc  = "${local.vpc_id}"
}

resource "aws_security_group" "service_egress_security_group" {
  name        = "${local.namespace}_service_egress_security_group"
  description = "Allow traffic between services"
  vpc_id      = "${local.vpc_id}"

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags {
    Name = "${local.namespace}-egress"
  }
}

# Messaging - archiver

module "archiver_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "${local.namespace}_archiver"
}

module "archiver_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v9.1.0"
  queue_name  = "${local.namespace}_archiver_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.archiver_topic.name}"]

  visibility_timeout_seconds = 43200
  max_receive_count          = 3

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}

# Messaging - registrar

module "registrar_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "${local.namespace}_registrar"
}

module "registrar_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v9.1.0"
  queue_name  = "${local.namespace}_registrar_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.registrar_topic.name}"]

  visibility_timeout_seconds = 43200
  max_receive_count          = 3

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}

# Archive bucket

# TODO: Add proper lifecycyle policy to prevent deletion and move to assets?
resource "aws_s3_bucket" "archive_storage" {
  bucket = "${local.archive_bucket_name}"
  acl    = "private"
}

data "aws_iam_policy_document" "archive_upload" {
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

resource "aws_iam_role_policy" "archive_archive_upload_bucket" {
  role   = "${module.archiver.task_role_name}"
  policy = "${data.aws_iam_policy_document.archive_upload.json}"
}

# Ingest bucket

resource "aws_s3_bucket" "ingest_storage" {
  bucket = "${local.ingest_bucket_name}"
  acl    = "private"
}

# TODO: Needs a lifecyle policy which deletes old stuff
data "aws_iam_policy_document" "archive_ingest" {
  statement {
    actions = [
      "s3:GetObject*",
    ]

    resources = [
      "arn:aws:s3:::${local.ingest_bucket_name}/*",
    ]
  }
}

resource "aws_iam_role_policy" "archive_archive_ingest_bucket" {
  role   = "${module.archiver.task_role_name}"
  policy = "${data.aws_iam_policy_document.archive_ingest.json}"
}

# Service - archiver

module "ecr_repository_archiver" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "archiver"
}

module "archiver" {
  source = "service"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  cluster_name                     = "${aws_ecs_cluster.cluster.name}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets                          = "${local.private_subnets}"
  vpc_id                           = "${local.vpc_id}"
  service_name                     = "${local.namespace}"
  aws_region                       = "${var.aws_region}"
  max_capacity                     = 1

  env_vars = {
    queue_url      = "${module.archiver_queue.id}"
    archive_bucket = "${aws_s3_bucket.archive_storage.id}"
  }

  env_vars_length = 2

  container_image   = "${local.archiver_container_image}"
  source_queue_name = "${module.archiver_queue.name}"
  source_queue_arn  = "${module.archiver_queue.arn}"
}

# Service - registrar

module "ecr_repository_registrar" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "registrar"
}

module "registrar" {
  source = "service"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  cluster_name                     = "${aws_ecs_cluster.cluster.name}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets                          = "${local.private_subnets}"
  vpc_id                           = "${local.vpc_id}"
  service_name                     = "${local.namespace}"
  aws_region                       = "${var.aws_region}"
  max_capacity                     = 1

  env_vars = {
    queue_url      = "${module.registrar_queue.id}"
    archive_bucket = "${aws_s3_bucket.archive_storage.id}"
  }

  env_vars_length = 2

  container_image   = "${local.registrar_container_image}"
  source_queue_name = "${module.registrar_queue.name}"
  source_queue_arn  = "${module.registrar_queue.arn}"
}
