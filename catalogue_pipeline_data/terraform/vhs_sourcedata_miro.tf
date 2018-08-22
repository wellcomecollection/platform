module "vhs_miro" {
  source = "./vhs"
  name   = "sourcedata-miro"

  table_write_max_capacity = 750
}
