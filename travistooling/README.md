# travistooling

This directory contains scripts for managing our Travis build tasks, tests,
publishes to ECR, etc.  It's very specific to the Platform repo.

## Running tests

As with all Platform tests, you can run tests with a Make task:

```console
$ make travistooling-test
```

The test suite requires 100% line and branch coverage (which is enforced
by [coverage.py](http://coverage.readthedocs.io/en/latest/)).

Note: there is one known failure when running tests locally:

*   `test_unpack_secrets()` relies on encrypted environment variables which
    are only available in Travis, not for local dev.  This test should pass
    when running in Travis.

## Rebuilding the secrets file

The secrets ZIP file contains three files:

-  _secrets/config_

    An AWS configuration file.  Last time I edited the file, this is what
    it contained:

    ```ini
    [default]
    region = eu-west-1
    ```

*   _secrets/credentials_

    An AWS credentials file.  It has the following format, with credentials
    provided by the shared-infra stack:

    ```ini
    [default]
    aws_access_key_id=<ACCESS_KEY_ID>
    aws_secret_access_key=<SECRET_ACCESS_KEY>
    ```

*   _secrets/id_rsa_

    An SSH private key.  The corresponding public key should be added as a
    deploy key to the platform repo with write access.  If you don't have
    the existing key, you can create a new key pair with:

    ```console
    $ ssh-keygen -N "" -f secrets/id_rsa
    ```

Once you have all of these files, you create the encrypted bundle by running:

```console
$ zip -r secrets.zip secrets
$ travis encrypt-file secrets.zip
```

This creates a file `secrets.zip.enc`, which you should commit to the
repository.

You'll be shown an OpenSSL command for unpacking this file -- check it matches
the command we run in `unpack_secrets()` in `travis_utils.py`.
