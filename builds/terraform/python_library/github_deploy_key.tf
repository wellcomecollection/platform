# The docs for this resource say pretty clearly:
#
#     Usage in production deployments is not recommended
#
# Since this key is only used to grant push access to a couple of
# non-critical repos, I'll take the risk.

resource "tls_private_key" "github_key" {
  algorithm = "RSA"
}

resource "github_repository_deploy_key" "deploy_key" {
  title      = "deploy_key from terraform"
  repository = var.repo_name
  key        = tls_private_key.github_key.public_key_openssh
  read_only  = false
}
