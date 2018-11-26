module "vhs_archive_manifest" {
  source = "modules/vhs"
  name   = "archive-manifests"

  table_read_max_capacity  = 30
  table_write_max_capacity = 30
}