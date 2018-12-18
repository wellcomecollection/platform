output "integration_uris" {
  value = [
    "${module.auth_subresource_integration.uri}",
    "${module.auth_resource_integration.uri}",
  ]
}
