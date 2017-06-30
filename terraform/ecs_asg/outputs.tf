output "instance_sg_id" {
  value = "${aws_security_group.instance_sg.id}"
}

output "loadbalancer_sg_https_id" {
  value = "${aws_security_group.https.id}"
}

output "loadbalancer_sg_http_id" {
  value = "${aws_security_group.http.id}"
}
