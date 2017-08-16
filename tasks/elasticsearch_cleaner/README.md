# elasticsearch_cleaner

This service deletes unused indices from our Elasticsearch cluster.

As we update our data model, we're indexing our data into new Elasticsearch indices (whether using the Elastic APIs or our reindexer service).
Deleting the old indices frees up space on the cluster.

## Usage

Within this directory, build the Docker image containing the script:

```console
$ make build
```

This builds a Docker image called `elasticsearch_cleaner`.
Run the image as follows:

```console
$ $(git rev-parse --show-toplevel)/scripts/run_docker_with_aws_credentials.sh -e BUCKET=platform-infra -e KEY=terraform.tfvars -e DRY_RUN=false elasticsearch_cleaner
```

## Deployment

This task isn't automatically built or deployed by Travis, because it changes fairly infrequently.

Within this directory, run the `deploy` command:

```console
$ make deploy
```
