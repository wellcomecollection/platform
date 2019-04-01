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

Copy each of these to the corresponding repo as `secrets.zip`, then use the Travis CLI to encrypt them:

```console
$ travis encrypt-file secrets.zip
```

### Secrets files

When you run `make builds-terraform-apply`, you'll see a number of `secrets_<name>.zip` files get created in the `scala_library` module.

You should copy these into the corrsponding repo, run `travis encrypt-file`, and commit the result.  Make the public key it contains a deploy key on GitHub with write access.
