locals {
  # Used by the merger applications, which all have singular names
  resource_type_singular = "${replace("${var.resource_type}", "s", "")}"
}
