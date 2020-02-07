resource "aws_iam_role_policy_attachment" "test-attach" {
  role       = var.role_name
  policy_arn = "arn:aws:iam::aws:policy/ReadOnlyAccess"
}