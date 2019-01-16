locals {
  namespace = "catalogue_${replace(var.date_created, "-", "")}"

  es_index_v1 = "v1-${var.date_created}-${var.name}"
  es_index_v2 = "v2-${var.date_created}-${var.name}"
}
