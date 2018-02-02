module "versioned-hybrid-store" {
  source      = "vhs"
  bucket_name = "wellcomecollection-source-data"
  table_name  = "SourceData"
}
