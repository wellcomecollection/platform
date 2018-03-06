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

    make loris-run

You'll then have a development version of Loris running at <http://localhost:8888>.

## Testing Loris with nginx

In order to test Loris with its nginx config this project provides a `docker-compose.yml`

This can be run using (substituting env variables as necessary):

### docker-compose

This starts both loris and nginx (at the tag specified), using the config file specified.

```sh
CONFIG_KEY=location/of/config.ini \
NGINX_TAG=latest \
AWS_ACCESS_KEY_ID=key_id \
AWS_SECRET_ACCESS_KEY=access_key \
docker-compose up

```

### curl localhost:9000

You can then query the local version of loris (in order to avoid redirects it's necessary to use the `ELB-HealthChecker/2.0` User-Agent).

```sh
curl \
    --user-agent ELB-HealthChecker/2.0 \
    --verbose \
    --output default.jpg \
    http://localhost:9000/image/V0017087.jpg/full/300,/0/default.jpg
```