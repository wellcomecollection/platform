module "super_dev_roleset" {
  source = "./modules/roleset"

  name = "platform-superdev"

  federated_principal = module.account_federation.principal
  aws_principal       = local.aws_principal

  assumable_role_arns = [
    # Platform
    module.aws_account.admin_role_arn,

    module.aws_account.developer_role_arn,
    module.aws_account.read_only_role_arn,

    # Workflow
    module.workflow_account.admin_role_arn,
    module.workflow_account.developer_role_arn,
    module.workflow_account.read_only_role_arn,

    module.workflow_support_role.arn,

    # Storage
    module.storage_account.admin_role_arn,
    module.storage_account.developer_role_arn,
    module.storage_account.read_only_role_arn,

    # Experience
    module.experience_account.admin_role_arn,
    module.experience_account.developer_role_arn,
    module.experience_account.read_only_role_arn,

    # Data
    module.data_account.developer_role_arn,
    module.data_account.read_only_role_arn,
    module.data_account.admin_role_arn,

    # Reporting
    module.reporting_account.developer_role_arn,
    module.reporting_account.read_only_role_arn,
    module.reporting_account.admin_role_arn,

    # Digitisation
    module.digitisation_account.developer_role_arn,
    module.digitisation_account.read_only_role_arn,
    module.digitisation_account.admin_role_arn,

    # Catalogue
    module.catalogue_account.developer_role_arn,
    module.catalogue_account.read_only_role_arn,
    module.catalogue_account.admin_role_arn,

    # CI Roles
    module.aws_account.publisher_role_arn,

    aws_iam_role.s3_scala_releases_read.arn,
    module.s3_releases_scala_sierra_client.role_arn,
    module.s3_releases_scala_catalogue_client.role_arn,
    module.s3_releases_scala_storage.role_arn,
    module.s3_releases_scala_json.role_arn,
    module.s3_releases_scala_messaging.role_arn,
    module.s3_releases_scala_monitoring.role_arn,
    module.s3_releases_scala_typesafe.role_arn,
    module.s3_releases_scala_fixtures.role_arn,

    # Route 53 - drupalinfra
    "arn:aws:iam::250790015188:role/wellcomecollection-assume_role_hosted_zone_update",
  ]
}

module "dev_roleset" {
  source = "./modules/roleset"

  name = "platform-dev"

  federated_principal = module.account_federation.principal
  aws_principal       = local.aws_principal

  assumable_role_arns = [
    # Platform
    module.aws_account.developer_role_arn,
    module.aws_account.read_only_role_arn,

    # Workflow
    module.workflow_account.developer_role_arn,
    module.workflow_account.read_only_role_arn,

    # Storage
    module.storage_account.developer_role_arn,
    module.storage_account.read_only_role_arn,

    # Experience
    module.experience_account.developer_role_arn,
    module.experience_account.read_only_role_arn,

    # Data
    module.data_account.developer_role_arn,
    module.data_account.read_only_role_arn,

    # Reporting
    module.reporting_account.developer_role_arn,
    module.reporting_account.read_only_role_arn,

    # Catalogue
    module.catalogue_account.developer_role_arn,
    module.catalogue_account.read_only_role_arn,

    # Digitisation
    module.digitisation_account.developer_role_arn,
    module.digitisation_account.read_only_role_arn,

    # Scala lib read Role
    aws_iam_role.s3_scala_releases_read.arn,

    # CI Roles
    module.aws_account.publisher_role_arn,

    module.s3_releases_scala_fixtures.role_arn,
    module.s3_releases_scala_json.role_arn,
    module.s3_releases_scala_messaging.role_arn,
    module.s3_releases_scala_monitoring.role_arn,
    module.s3_releases_scala_storage.role_arn,
    module.s3_releases_scala_typesafe.role_arn,
  ]
}

module "storage_dev_roleset" {
  source = "./modules/roleset"

  name = "storage-dev"

  federated_principal = module.account_federation.principal
  aws_principal       = local.aws_principal

  assumable_role_arns = [
    # Platform
    module.aws_account.read_only_role_arn,

    # Workflow
    module.workflow_account.developer_role_arn,
    module.workflow_account.read_only_role_arn,

    # Storage
    module.storage_account.developer_role_arn,
    module.storage_account.read_only_role_arn,

    # Scala lib read Role
    aws_iam_role.s3_scala_releases_read.arn,
  ]
}

module "workflow_dev_roleset" {
  source = "./modules/roleset"

  name = "workflow-dev"

  federated_principal = module.account_federation.principal
  aws_principal       = local.aws_principal

  assumable_role_arns = [
    # Workflow
    module.workflow_account.admin_role_arn,
    module.workflow_account.developer_role_arn,
    module.workflow_account.read_only_role_arn,

    module.workflow_support_role.arn,
  ]
}

module "data_analyst_roleset" {
  source = "./modules/roleset"

  name = "data-analyst"

  federated_principal = module.account_federation.principal
  aws_principal       = local.aws_principal

  assumable_role_arns = [
    module.aws_account.read_only_role_arn,
    module.experience_account.read_only_role_arn,
    module.workflow_account.read_only_role_arn,

    module.storage_account.read_only_role_arn,
    module.reporting_account.read_only_role_arn,
    module.data_account.read_only_role_arn,
  ]
}

module "data_dev_roleset" {
  source = "./modules/roleset"

  name = "data-dev"

  federated_principal = module.account_federation.principal
  aws_principal       = local.aws_principal

  assumable_role_arns = [
    # Platform
    module.aws_account.developer_role_arn,
    module.aws_account.read_only_role_arn,

    # Data
    module.data_account.admin_role_arn,
    module.data_account.developer_role_arn,
    module.data_account.read_only_role_arn,

    # Reporting
    module.reporting_account.developer_role_arn,
    module.reporting_account.read_only_role_arn,

    # Scala lib read Role
    aws_iam_role.s3_scala_releases_read.arn,
  ]
}

module "digitisation_dev_roleset" {
  source = "./modules/roleset"

  name = "digitisation-dev"

  federated_principal = module.account_federation.principal
  aws_principal       = local.aws_principal

  assumable_role_arns = [
    # Platform
    module.aws_account.read_only_role_arn,

    # Digitisation
    module.digitisation_account.developer_role_arn,
    module.digitisation_account.read_only_role_arn,

    # Workflow
    module.workflow_account.read_only_role_arn,
    module.workflow_support_role.arn,

    # Storage
    module.storage_account.read_only_role_arn,

    # Scala lib read Role
    aws_iam_role.s3_scala_releases_read.arn,
  ]
}

module "digitisation_admin_roleset" {
  source = "./modules/roleset"

  name = "digitisation-admin"

  federated_principal = module.account_federation.principal
  aws_principal       = local.aws_principal

  assumable_role_arns = [
    # Platform
    module.aws_account.read_only_role_arn,

    # Digitisation
    module.digitisation_account.admin_role_arn,
    module.digitisation_account.developer_role_arn,
    module.digitisation_account.read_only_role_arn,

    # Workflow
    module.workflow_account.read_only_role_arn,
    module.workflow_support_role.arn,

    # Storage
    module.storage_account.read_only_role_arn,

    # Scala lib read Role
    aws_iam_role.s3_scala_releases_read.arn,
  ]
}
