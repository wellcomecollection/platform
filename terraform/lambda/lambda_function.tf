resource "aws_lambda_function" "lambda_function" {
  description   = "${var.description}"
  filename      = "${var.filename}"
  function_name = "${var.name}"
  role          = "${aws_iam_role.iam_role.arn}"
  handler       = "${var.name}.main"
  runtime       = "python2.7"
}
