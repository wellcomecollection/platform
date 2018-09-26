output "id" {
  value = "${aws_alb.api_delta.id}"
}

output "https_listener_arn" {
  value = "${aws_alb_listener.https.arn}"
}

output "http_listener_arn" {
  value = "${aws_lb_listener.http.arn}"
}

output "cloudwatch_id" {
  value = "${aws_alb.api_delta.arn_suffix}"
}

output "service_lb_security_group_id" {
  value = "${aws_security_group.service_lb_security_group.id}"
}