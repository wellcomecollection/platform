resource "aws_kms_key" "encryption_key" {
  description = "KMS key for encrypting in the storage stack"
  policy      = "${data.aws_iam_policy_document.enable_kms_iam.json}"
}
