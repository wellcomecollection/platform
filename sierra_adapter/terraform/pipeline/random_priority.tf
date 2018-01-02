resource "random_integer" "priority_sierra_to_dynamo" {
  min = 1
  max = 50000

  keepers = {
    listener_https_arn = "${var.alb_listener_https_arn}"
    listener_http_arn  = "${var.alb_listener_http_arn}"
  }

  seed = "${var.name}_sierra_${var.resource_type}_to_dynamo"
}

resource "random_integer" "priority_sierra_merger" {
  min = 1
  max = 50000

  keepers = {
    listener_https_arn = "${var.alb_listener_https_arn}"
    listener_http_arn  = "${var.alb_listener_http_arn}"
  }

  seed = "${var.name}_sierra_${var.resource_type}_merger"
}
