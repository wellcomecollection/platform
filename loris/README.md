# loris

This directory has the infrastructure for our [Loris][loris] deployment.

[loris]: https://github.com/loris-imageserver/loris

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