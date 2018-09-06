module "vhs_recorder" {
  source = "../../../catalogue_pipeline_data/terraform/vhs"
  name   = "${replace(var.namespace, "_", "-")}-Recorder"

  table_stream_enabled = false
}
