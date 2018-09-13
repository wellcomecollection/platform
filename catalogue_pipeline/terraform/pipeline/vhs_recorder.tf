
module "vhs_recorder" {
  source = "../../../catalogue_pipeline_data/terraform/vhs"
  name   = "${replace(var.namespace, "_", "-")}-recorder"

  table_read_max_capacity  = 1000
  table_write_max_capacity = 300

  prevent_destroy = "false"

  account_id = "${var.account_id}"
  bucket_name = "${var.vhs_bucket_name}"
}
