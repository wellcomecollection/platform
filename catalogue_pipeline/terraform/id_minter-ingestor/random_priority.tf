resource "random_integer" "priority_id_minter" {
  min = 1
  max = 50000

  keepers = {
    listener_https_arn = "${var.services_alb["listener_https_arn"]}"
    listener_http_arn  = "${var.services_alb["listener_http_arn"]}"
  }

  seed = "${var.name}_id_minter"
}

resource "random_integer" "priority_ingestor" {
  min = 1
  max = 50000

  keepers = {
    listener_https_arn = "${var.services_alb["listener_https_arn"]}"
    listener_http_arn  = "${var.services_alb["listener_http_arn"]}"
  }

  seed = "${var.name}_ingestor"
}
