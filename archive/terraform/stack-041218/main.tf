# archivist

module "archivist" {
  source = "../modules/service/worker"

  service_egress_security_group_id = "${var.service_egress_security_group_id}"
  cluster_name                     = "${aws_ecs_cluster.cluster.name}"
  cluster_id                       = "${aws_ecs_cluster.cluster.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets                          = "${var.private_subnets}"
  vpc_id                           = "${var.vpc_id}"
  service_name                     = "${var.namespace}-archivist"
  aws_region                       = "${var.aws_region}"

  env_vars = {
    queue_url           = "${module.archivist_queue.url}"
    archive_bucket      = "${var.archive_bucket_name}"
    registrar_topic_arn = "${module.bags_topic.arn}"
    progress_topic_arn  = "${module.ingests_topic.arn}"
  }

  env_vars_length = 4

  min_capacity = "4"
  max_capacity = "4"

  cpu    = "1900"
  memory = "14000"

  container_image = "${var.archivist_container_image}"

  # The maximum number of tasks is limited by the availability of network interfaces
  launch_type = "EC2"
}

# bags aka registrar-async

module "bags" {
  source = "../modules/service/worker"

  service_egress_security_group_id = "${var.service_egress_security_group_id}"
  cluster_name                     = "${aws_ecs_cluster.cluster.name}"
  cluster_id                       = "${aws_ecs_cluster.cluster.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets                          = "${var.private_subnets}"
  vpc_id                           = "${var.vpc_id}"
  service_name                     = "${var.namespace}-bags_async"
  aws_region                       = "${var.aws_region}"

  env_vars = {
    queue_url          = "${module.bags_queue.url}"
    archive_bucket     = "${var.archive_bucket_name}"
    progress_topic_arn = "${module.ingests_topic.arn}"
    vhs_bucket_name    = "${var.vhs_archive_manifest_bucket_name}"
    vhs_table_name     = "${var.vhs_archive_manifest_table_name}"
  }

  env_vars_length = 5

  container_image = "${var.registrar_async_container_image}"
}

# notifier

module "notifier" {
  source = "../modules/service/worker"

  service_egress_security_group_id = "${var.service_egress_security_group_id}"
  security_group_ids               = ["${var.interservice_security_group_id}"]

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = "${var.private_subnets}"
  vpc_id       = "${var.vpc_id}"
  service_name = "${var.namespace}-notifier"
  aws_region   = "${var.aws_region}"

  env_vars = {
    context_url        = "https://api.wellcomecollection.org/storage/v1/context.json"
    notifier_queue_url = "${module.notifier_queue.url}"
    progress_topic_arn = "${module.ingests_topic.arn}"
  }

  env_vars_length = 3

  container_image = "${var.notifier_container_image}"
}

# ingests aka progress-async

module "ingests" {
  source = "../modules/service/worker"

  service_egress_security_group_id = "${var.service_egress_security_group_id}"
  cluster_name                     = "${aws_ecs_cluster.cluster.name}"
  cluster_id                       = "${aws_ecs_cluster.cluster.id}"

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = "${var.private_subnets}"
  vpc_id       = "${var.vpc_id}"
  service_name = "${var.namespace}-ingests_async"
  aws_region   = "${var.aws_region}"

  env_vars = {
    queue_url                   = "${module.ingests_queue.url}"
    topic_arn                   = "${module.notifier_topic.arn}"
    archive_progress_table_name = "${aws_dynamodb_table.archive_progress_table.name}"
  }

  env_vars_length = 3

  container_image = "${var.progress_async_container_image}"
}

# Storage API

module "api" {
  source = "api"

  vpc_id       = "${var.vpc_id}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  cluster_name = "${aws_ecs_cluster.cluster.name}"
  subnets      = "${var.private_subnets}"

  domain_name = "${var.domain_name}"

  namespace     = "${var.namespace}-api"
  namespace_id  = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  namespace_tld = "${aws_service_discovery_private_dns_namespace.namespace.name}"

  # Auth

  auth_scopes = [
    "${var.cognito_storage_api_identifier}/ingests",
    "${var.cognito_storage_api_identifier}/bags",
  ]
  cognito_user_pool_arn = "${var.cognito_user_pool_arn}"

  # Bags endpoint

  bags_container_image = "${var.registrar_http_container_image}"
  bags_container_port  = "9001"
  bags_env_vars = {
    context_url     = "https://api.wellcomecollection.org/storage/v1/context.json"
    vhs_bucket_name = "${var.vhs_archive_manifest_bucket_name}"
    vhs_table_name  = "${var.vhs_archive_manifest_table_name}"
    app_base_url    = "https://api.wellcomecollection.org/storage/v1/bags"
  }
  bags_env_vars_length       = 4
  bags_nginx_container_image = "${var.nginx_container_image}"
  bags_nginx_container_port  = "9000"

  # Ingests endpoint

  ingests_container_image = "${var.progress_http_container_image}"
  ingests_container_port  = "9001"
  ingests_env_vars = {
    context_url                 = "https://api.wellcomecollection.org/storage/v1/context.json"
    app_base_url                = "https://api.wellcomecollection.org/storage/v1/ingests"
    topic_arn                   = "${module.ingest_requests_topic.arn}"
    archive_progress_table_name = "${aws_dynamodb_table.archive_progress_table.name}"
  }
  ingests_env_vars_length            = 4
  ingests_nginx_container_image      = "${var.nginx_container_image}"
  ingests_nginx_container_port       = "9000"
  storage_static_content_bucket_name = "${var.storage_static_content_bucket_name}"
  interservice_security_group_id     = "${var.interservice_security_group_id}"
}

# Migration services

module "bagger" {
  source = "../modules/service/worker"

  service_egress_security_group_id = "${var.service_egress_security_group_id}"
  cluster_name                     = "${aws_ecs_cluster.cluster.name}"
  cluster_id                       = "${aws_ecs_cluster.cluster.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets                          = "${var.private_subnets}"
  vpc_id                           = "${var.vpc_id}"
  service_name                     = "${var.namespace}-bagger"
  aws_region                       = "${var.aws_region}"

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

  container_image = "${var.bagger_container_image}"
}

module "migrator" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/prebuilt/vpc?ref=v16.1.2"

  name        = "${var.namespace}-migrator"
  description = "Passes on the location of a successfully bagged set of METS and objects to the Archive Ingest API"

  timeout = 25

  environment_variables = {
    # Private DNS
    INGEST_API_URL = "http://storage-api-ingests.archive-storage:9000/progress"
    ARCHIVE_SPACE  = "digitised"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/archive/migrator.zip"

  security_group_ids = [
    "${var.interservice_security_group_id}",
    "${var.service_egress_security_group_id}",
  ]

  subnet_ids = "${var.private_subnets}"

  log_retention_in_days = 30
}

module "trigger_migrator" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/modules/triggers/sns?ref=v16.1.2"

  lambda_function_name = "${module.migrator.function_name}"
  sns_trigger_arn      = "${module.bagging_complete_topic.arn}"
}
