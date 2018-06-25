module "vhs_sourcedata" {
  source = "./vhs"

  name              = "SourceData"
  table_name_prefix = ""

  table_write_max_capacity = 750
}
