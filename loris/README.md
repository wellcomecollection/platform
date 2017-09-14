# loris

This directory has the Docker image and infrastructure for our [Loris][loris] deployment.

[loris]: https://github.com/loris-imageserver/loris

## Updating the installed version of Loris

We install Loris directly from GitHub, rather than a versioned release -- this allows us to deploy fixes immediately.
What we install is decided by two environment variables in the Dockerfile: `LORIS_GITHUB_USER` and `LORIS_COMMIT`.
When set, these install Loris from the following URL:

https://github.com/:LORIS_GITHUB_USER/loris/commit/:LORIS_COMMIT

Note that `LORIS_GITHUB_USER` should usually be `loris-imageserver` (pointing to the main Loris repo) unless we're temporarily deploying from a fork.

Once you've changed these variables, the new version of Loris will be installed the next time you run `make loris-build`.

## Running Loris in development mode

Sometimes it can be convenient to test Loris locally, without pushing a new commit to GitHub -- e.g. for testing a code patch, or experimenting with config.
You can run Loris locally with the following two commands:

    make loris-build

    docker run \
        --volume $PLATFORM/terraform/services/config/templates/loris.ini.template:/opt/loris/etc/loris2.conf \
        --volume $LORIS/loris:/usr/local/lib/python2.7/dist-packages/Loris-2.0.0-py2.7.egg/loris \
        --volume $LORIS/www/icons:/var/www/loris2/icons \
        --publish 8888:8888 loris

where `$PLATFORM` is the root of the platform repo, and `$LORIS` is the root of a local checkout of the Loris repo.

You'll then have a development version of Loris running at <http://localhost:8888>.
