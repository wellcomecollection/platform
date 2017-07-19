output "listener_https_arn" {
  value = "${aws_alb_listener.https.arn}"
}

output "listener_http_arn" {
  value = "${aws_alb_listener.http.arn}"
}

output "cloudwatch_id" {
  value = "${aws_alb.ecs_service.arn_suffix}"
}

output "dns_name" {
  value = "${aws_alb.ecs_service.dns_name}"
}
