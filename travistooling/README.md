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
