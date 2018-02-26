module "vhs_sourcedata" {
  source      = "vhs"
  bucket_name = "wellcomecollection-source-data"
  table_name  = "SourceData"
}
