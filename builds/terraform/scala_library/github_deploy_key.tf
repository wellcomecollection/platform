resource "github_repository_deploy_key" "deploy_key" {
  title      = "deploy_key from terraform"
  repository = "scala-${var.name}"
  key        = "${tls_private_key.github_key.public_key_openssh}"
  read_only  = false
}
