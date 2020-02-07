output "photography_backups_access_id" {
  value = aws_iam_access_key.photography_backups.id
}

output "photography_backups_encrypted_secret" {
  value = aws_iam_access_key.photography_backups.encrypted_secret
}
