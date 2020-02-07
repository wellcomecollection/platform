# Scala library publisher user

module "ci_machine" {
  source = "./modules/users/machine"

  pgp_key = data.template_file.pgp_key.rendered
  prefix  = "ci"

  assumable_roles = [
    module.aws_account.publisher_role_arn,

    module.s3_releases_scala_fixtures.role_arn,
    module.s3_releases_scala_json.role_arn,
    module.s3_releases_scala_messaging.role_arn,
    module.s3_releases_scala_monitoring.role_arn,
    module.s3_releases_scala_storage.role_arn,
    module.s3_releases_scala_typesafe.role_arn,
    module.s3_releases_scala_catalogue_client.role_arn,
  ]
}

