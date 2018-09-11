module "vhs_recorder" {
  source = "../../catalogue_pipeline_data/terraform/vhs"
  name   = "catalogue-pipeline-recorder"

  table_read_max_capacity  = 1000
  table_write_max_capacity = 300

  table_stream_enabled = false
}
