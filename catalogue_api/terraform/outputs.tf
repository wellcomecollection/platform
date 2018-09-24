output "romulus_app_uri" {
  value = "${local.romulus_app_uri}"
}

output "remus_app_uri" {
  value = "${local.remus_app_uri}"
}

output "snapshots_bucket_arn" {
  value = "${module.data_api.snapshots_bucket_arn}"
}
