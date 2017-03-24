# tools

# terraform

We use [Terraform][terraform] for managing our infrastructure.  All our
Terraform code is in the [platform-infra repo][infra].

You can install Terraform with Homebrew:

    brew install terraform

Upgrading is likewise done through homebrew, but if you upgrade terraform then
push remote state, everybody else will have to upgrade as well!  Remote state
isn't backward compatible (at least, not yet).

We use the built-in auto-formatter for our Terraform code:

    terraform fmt

Run this over your patches to ensure consistent code style.

[terraform]: https://www.terraform.io/
[infra]: https://github.com/wellcometrust/platform-infra
