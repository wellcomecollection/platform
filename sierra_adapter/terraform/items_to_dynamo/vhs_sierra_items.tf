module "vhs_sierra_items" {
  source = "../../../catalogue_pipeline_data/terraform/vhs"
  name   = "sourcedata-sierra-items"

  table_stream_enabled = false
}
