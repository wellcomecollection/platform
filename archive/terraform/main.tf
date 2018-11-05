# API

module "api_ecs" {
  source = "api_ecs"

  namespace = "${local.namespace}"
  api_path  = "/storage/v1"

  archive_api_container_image = "${local.api_ecs_container_image}"
  archive_api_container_port  = "9000"

  vpc_id                         = "${local.vpc_id}"
  interservice_security_group_id = "${aws_security_group.interservice_security_group.id}"
  private_subnets                = "${local.private_subnets}"
  public_subnets                 = "${local.public_subnets}"
  certificate_domain             = "api.wellcomecollection.org"
  api_alb_cdir_blocks            = "${var.api_alb_cdir_blocks}"
}

# Archivist

module "archivist" {
  source = "internal_queue_service"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  cluster_name                     = "${aws_ecs_cluster.cluster.name}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets                          = "${local.private_subnets}"
  vpc_id                           = "${local.vpc_id}"
  service_name                     = "archivist"
  aws_region                       = "${var.aws_region}"

  min_capacity = 1
  max_capacity = 1

  env_vars = {
    queue_url           = "${module.archivist_queue.id}"
    archive_bucket      = "${aws_s3_bucket.archive_storage.id}"
    registrar_topic_arn = "${module.registrar_topic.arn}"
    progress_topic_arn  = "${module.progress_async_topic.arn}"
  }

  env_vars_length = 4

  container_image   = "${local.archivist_container_image}"
  source_queue_name = "${module.archivist_queue.name}"
  source_queue_arn  = "${module.archivist_queue.arn}"
}

# Registrar

module "registrar_async" {
  source = "internal_queue_service"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  cluster_name                     = "${aws_ecs_cluster.cluster.name}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets                          = "${local.private_subnets}"
  vpc_id                           = "${local.vpc_id}"
  service_name                     = "registrar_async"
  aws_region                       = "${var.aws_region}"

  min_capacity = 1
  max_capacity = 1

  env_vars = {
    queue_url          = "${module.registrar_queue.id}"
    archive_bucket     = "${aws_s3_bucket.archive_storage.id}"
    progress_topic_arn = "${module.progress_async_topic.arn}"
    vhs_bucket_name    = "${module.vhs_archive_manifest.bucket_name}"
    vhs_table_name     = "${module.vhs_archive_manifest.table_name}"
  }

  env_vars_length = 5

  container_image   = "${local.registrar_async_container_image}"
  source_queue_name = "${module.registrar_queue.name}"
  source_queue_arn  = "${module.registrar_queue.arn}"
}

module "registrar_http" {
  source       = "internal_rest_service"
  service_name = "registrar_http"

  container_port  = "9001"
  container_image = "${local.registrar_http_container_image}"

  env_vars = {
    vhs_bucket_name = "${module.vhs_archive_manifest.bucket_name}"
    vhs_table_name  = "${module.vhs_archive_manifest.table_name}"
    app_base_url    = "https://api.wellcomecollection.org"
  }

  env_vars_length = 3

  security_group_ids = ["${aws_security_group.service_egress_security_group.id}", "${aws_security_group.interservice_security_group.id}", "${aws_security_group.tcp_access_security_group.id}"]
  private_subnets    = "${local.private_subnets}"

  cluster_id = "${aws_ecs_cluster.cluster.id}"
  vpc_id     = "${local.vpc_id}"

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
}

# Notifier

module "notifier" {
  source = "internal_queue_service"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  security_group_ids               = ["${aws_security_group.interservice_security_group.id}"]
  cluster_name                     = "${aws_ecs_cluster.cluster.name}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets                          = "${local.private_subnets}"
  vpc_id                           = "${local.vpc_id}"
  service_name                     = "notifier"
  aws_region                       = "${var.aws_region}"

  min_capacity = 1
  max_capacity = 1

  env_vars = {
    notifier_queue_url = "${module.notifier_queue.id}"
    progress_topic_arn = "${module.progress_async_topic.arn}"
  }

  env_vars_length = 2

  container_image   = "${local.notifier_container_image}"
  source_queue_name = "${module.notifier_queue.name}"
  source_queue_arn  = "${module.notifier_queue.arn}"
}

# Progress

module "progress_async" {
  source = "internal_queue_service"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  cluster_name                     = "${aws_ecs_cluster.cluster.name}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets                          = "${local.private_subnets}"
  vpc_id                           = "${local.vpc_id}"
  service_name                     = "progress_async"
  aws_region                       = "${var.aws_region}"

  min_capacity = 1
  max_capacity = 1

  env_vars = {
    queue_url                   = "${module.progress_async_queue.id}"
    topic_arn                   = "${module.notifier_topic.arn}"
    archive_progress_table_name = "${aws_dynamodb_table.archive_progress_table.name}"
  }

  env_vars_length = 3

  container_image   = "${local.progress_async_container_image}"
  source_queue_name = "${module.progress_async_queue.name}"
  source_queue_arn  = "${module.progress_async_queue.arn}"
}

module "progress_http" {
  source = "internal_rest_service"

  service_name = "progress_http"

  container_port  = "9001"
  container_image = "${local.progress_http_container_image}"

  env_vars = {
    app_base_url                = "https://${module.api_ecs.alb_dns_name}/storage/v1/ingests"
    topic_arn                   = "${module.ingest_requests_topic.arn}"
    archive_progress_table_name = "${aws_dynamodb_table.archive_progress_table.name}"
  }

  env_vars_length = 3

  security_group_ids = ["${aws_security_group.service_egress_security_group.id}", "${aws_security_group.interservice_security_group.id}", "${aws_security_group.tcp_access_security_group.id}"]
  private_subnets    = "${local.private_subnets}"

  cluster_id = "${aws_ecs_cluster.cluster.id}"
  vpc_id     = "${local.vpc_id}"

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
}

# Migration services

module "bagger" {
  source = "internal_queue_service"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  cluster_name                     = "${aws_ecs_cluster.cluster.name}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets                          = "${local.private_subnets}"
  vpc_id                           = "${local.vpc_id}"
  service_name                     = "bagger"
  aws_region                       = "${var.aws_region}"

  min_capacity = 1
  max_capacity = 1

  env_vars = {
    METS_BUCKET_NAME            = "${var.bagger_mets_bucket_name}"
    READ_METS_FROM_FILESHARE    = "${var.bagger_read_mets_from_fileshare}"
    WORKING_DIRECTORY           = "${var.bagger_working_directory}"
    DROP_BUCKET_NAME            = "${var.bagger_drop_bucket_name}"
    DROP_BUCKET_NAME_METS_ONLY  = "${var.bagger_drop_bucket_name_mets_only}"
    DROP_BUCKET_NAME_ERRORS     = "${var.bagger_drop_bucket_name_errors}"
    CURRENT_PRESERVATION_BUCKET = "${var.bagger_current_preservation_bucket}"
    DLCS_SOURCE_BUCKET          = "${var.bagger_dlcs_source_bucket}"
    BAGGING_QUEUE               = "${module.bagger_queue.name}"
    BAGGING_COMPLETE_TOPIC_ARN  = "${module.bagging_complete_topic.arn}"

    AWS_DEFAULT_REGION = "${var.aws_region}"

    # DLCS config
    DLCS_ENTRY       = "${var.bagger_dlcs_entry}"
    DLCS_API_KEY     = "${var.bagger_dlcs_api_key}"
    DLCS_API_SECRET  = "${var.bagger_dlcs_api_secret}"
    DLCS_CUSTOMER_ID = "${var.bagger_dlcs_customer_id}"
    DLCS_SPACE       = "${var.bagger_dlcs_space}"

    # DDS credentials
    DDS_API_KEY      = "${var.bagger_dds_api_key}"
    DDS_API_SECRET   = "${var.bagger_dds_api_secret}"
    DDS_ASSET_PREFIX = "${var.bagger_dds_asset_prefix}"
  }

  env_vars_length = 19

  container_image   = "${local.bagger_container_image}"
  source_queue_name = "${module.bagger_queue.name}"
  source_queue_arn  = "${module.bagger_queue.arn}"
}

module "migrator" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/prebuilt/vpc?ref=v11.12.0"

  name        = "migrator"
  description = "Passes on the location of a successfully bagged set of METS and objects to the Archive Ingest API"

  timeout = 25

  environment_variables = {
    # Private DNS
    INGEST_API_URL = "http://archive-storage_api.archive-storage_api:9000/storage/v1/ingests"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"

  s3_bucket = "${local.infra_bucket}"
  s3_key    = "lambdas/archive/migrator.zip"

  security_group_ids = [
    "${aws_security_group.interservice_security_group.id}",
    "${aws_security_group.service_egress_security_group.id}",
  ]

  subnet_ids = "${local.private_subnets}"

  log_retention_in_days = 30
}

module "trigger_migrator" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v10.2.2"

  lambda_function_arn  = "${module.migrator.arn}"
  lambda_function_name = "${module.migrator.function_name}"
  sns_trigger_arn      = "${module.bagging_complete_topic.arn}"
}

# Integration testing - callback_client

module "callback_stub_server" {
  source = "callback_service"

  service_name = "callback_stub_server"

  container_port  = "8080"
  container_image = "${local.callback_stub_server_container_image}"

  env_vars        = {}
  env_vars_length = 0
  command         = ["--verbose", "--disable-banner"]

  security_group_ids = ["${aws_security_group.service_egress_security_group.id}", "${aws_security_group.interservice_security_group.id}"]
  private_subnets    = "${local.private_subnets}"

  cluster_id = "${aws_ecs_cluster.cluster.id}"
  vpc_id     = "${local.vpc_id}"

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
}
