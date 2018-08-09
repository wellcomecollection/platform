output "test_base_url" {
  value = "${aws_api_gateway_deployment.test.invoke_url}"
}
