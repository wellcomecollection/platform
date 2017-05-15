data "archive_file" "lambda_zip_file" {
  type        = "zip"
  source_dir  = "${var.source_dir}"
  output_path = "/tmp/${var.name}.zip"
}

resource "aws_lambda_function" "lambda_function" {
  description   = "${var.description}"
  filename      = "${data.archive_file.lambda_zip_file.output_path}"
  function_name = "${var.name}"
  role          = "${aws_iam_role.iam_role.arn}"
  handler       = "${var.name}.main"
  runtime       = "python3.6"
}
