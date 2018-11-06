resource "aws_cognito_user_pool" "pool" {
  name = "Wellcome Collection"

  admin_create_user_config = {
    allow_admin_create_user_only = true
  }

  password_policy = {
    minimum_length = 8
  }
}

resource "aws_cognito_user_pool_domain" "domain" {
  domain       = "wellcomecollection"
  user_pool_id = "${aws_cognito_user_pool.pool.id}"
}

resource "aws_cognito_resource_server" "storage_api" {
  identifier = "https://api.wellcomecollection.org/storage/v1"
  name       = "Storage API V1"

  scope = [
    {
      scope_name        = "ingests"
      scope_description = "Read and write ingests"
    },
    {
      scope_name        = "bags"
      scope_description = "Read bags"
    },
  ]

  user_pool_id = "${aws_cognito_user_pool.pool.id}"
}

resource "aws_cognito_user_pool_client" "goobi" {
  name = "Goobi"

  allowed_oauth_flows = [
    "client_credentials",
  ]

  explicit_auth_flows = [
    "CUSTOM_AUTH_FLOW_ONLY",
  ]

  allowed_oauth_scopes = [
    "${aws_cognito_resource_server.storage_api.scope_identifiers}",
  ]

  allowed_oauth_flows_user_pool_client = true
  supported_identity_providers         = ["COGNITO"]

  generate_secret        = true
  refresh_token_validity = 1

  user_pool_id = "${aws_cognito_user_pool.pool.id}"
}

resource "aws_cognito_user_pool_client" "dds" {
  name = "DDS"

  allowed_oauth_flows = [
    "client_credentials",
  ]

  explicit_auth_flows = [
    "CUSTOM_AUTH_FLOW_ONLY",
  ]

  allowed_oauth_scopes = [
    "${aws_cognito_resource_server.storage_api.scope_identifiers}",
  ]

  allowed_oauth_flows_user_pool_client = true
  supported_identity_providers         = ["COGNITO"]

  generate_secret        = true
  refresh_token_validity = 1

  user_pool_id = "${aws_cognito_user_pool.pool.id}"
}
