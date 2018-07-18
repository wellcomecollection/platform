# builds

This directory contains the pieces for our Scala library builds.

### Secrets files

When you run `make builds-terraform-apply`, you'll see a number of `secrets_<name>.zip` files get created in the `scala_library` module.

You should copy these into the corrsponding repo, run `travis encrypt-file`, and commit the result.  Make the public key it contains a deploy key on GitHub with write access.
