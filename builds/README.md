# builds

This directory contains Terraform that's used in our build system.

In Travis CI, we have automated tooling that does things like:

*   Push commits to GitHub using a deploy key with write privileges
*   Publishes images to ECR and release IDs to SSM
*   Publishes Scala libraries to S3, and reads them back down
*   Publishes Python libraries to PyPI

This Terraform creates the credentials and "secrets.zip" file for use in each repository, that contains credentials with the right level of privileges to perform these operations.

### Usage

The terraform should be treated like any other stack:

```console
$ cd builds/terraform
$ terraform plan -out terraform.plan
$ terraform apply terraform.plan
```

When it's done, you'll see a number of `secrets_<reponame>.zip` files be created in the root of the `terraform` directory.

Copy each of these to the corresponding repo as `secrets.zip`, then use the [Travis CLI][travis] to encrypt them:

```console
$ travis encrypt-file secrets.zip
```

**Do not unpack the secrets files.**

**Do not check in the unencrypted zip files.**

[travis]: https://github.com/travis-ci/travis.rb

### If secrets get leaked

Revoke the credentials for the AWS user in the IAM console and recreate the user in Terraform.
The users have deliberately tight permissions to reduce the risk of malicious use.
Check CloudTrail for unexpected activity.

If they had SSH key access, check if it was revoked by GitHub or needs to be revoked manually (GitHub repo > settings > deploy keys).
Check the repo for unexpected commits.

If it included the PyPI password, rotate that and check for unexpected releases.
