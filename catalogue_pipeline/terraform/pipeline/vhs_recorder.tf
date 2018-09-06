module "vhs_recorder" {
  source = "../../../catalogue_pipeline_data/terraform/vhs"
  name   = "${replace(var.namespace, "_", "-")}-Recorder"

  table_read_max_capacity  = 1000
  table_write_max_capacity = 300

  table_stream_enabled = false
}
