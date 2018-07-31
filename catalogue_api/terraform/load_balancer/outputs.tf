output "id" {
  value = "${aws_alb.api_delta.id}"
}

output "https_listener_arn" {
  value = "${aws_alb_listener.https.arn}"
}

output "cloudwatch_id" {
  value = "${aws_alb.api_delta.arn_suffix}"
}
