INFRA_BUCKET = platform-infra


# Build the Docker images used for CI tasks.
#
# The script sticks a record that the image has been built in .docker, so the
# image isn't rebuilt unless you run 'make clean' first.  This makes CI tasks
# slightly less chatty when run locally.

clean:
	rm -rf .docker

.docker/jslint_ci:
	./scripts/build_ci_docker_image.py --project jslint_ci

.docker/python3.6_ci:
	./scripts/build_ci_docker_image.py --project python3.6_ci

.docker/terraform_ci:
	./scripts/build_ci_docker_image.py --project terraform_ci

.docker/packer_ci:
	./scripts/build_ci_docker_image.py --project packer_ci

.docker/_build_deps:
	pip3 install --upgrade boto3 docopt
	mkdir -p .docker && touch .docker/_build_deps


## Build the image for gatling
gatling-build: .docker/_build_deps
	./scripts/build_docker_image.py --project=gatling

## Deploy the image for gatling
gatling-deploy: gatling-build
	./scripts/deploy_docker_to_aws.py --project=gatling --infra-bucket=$(INFRA_BUCKET)


## Build the image for the cache cleaner
cache_cleaner-build: .docker/_build_deps
	./scripts/build_docker_image.py --project=cache_cleaner

## Deploy the image for the cache cleaner
cache_cleaner-deploy: cache_cleaner-build
	./scripts/deploy_docker_to_aws.py --project=cache_cleaner --infra-bucket=$(INFRA_BUCKET)


## Build the image for tif-metadata
tif-metadata-build: .docker/_build_deps
	./scripts/build_docker_image.py --project=tif-metadata

## Deploy the image for tif-metadata
tif-metadata-deploy: tif-metadata-build
	./scripts/deploy_docker_to_aws.py --project=tif-metadata --infra-bucket=$(INFRA_BUCKET)


## Build the image for Loris
loris-build: .docker/_build_deps
	./scripts/build_docker_image.py --project=loris

## Deploy the image for Loris
loris-deploy: loris-build
	./scripts/deploy_docker_to_aws.py --project=loris --infra-bucket=$(INFRA_BUCKET)


miro_adapter-build: .docker/_build_deps
	./scripts/build_docker_image.py --project=miro_adapter --file=miro_adapter/Dockerfile

miro_adapter-deploy: miro_adapter-build
	./scripts/deploy_docker_to_aws.py --project=miro_adapter --infra-bucket=$(INFRA_BUCKET)


elasticdump-build: .docker/_build_deps
	./scripts/build_docker_image.py --project=elasticdump

elasticdump-deploy: elasticdump-build
	./scripts/deploy_docker_to_aws.py --project=elasticdump --infra-bucket=$(INFRA_BUCKET)


nginx-build-api: .docker/_build_deps
	./scripts/build_docker_image.py --project=nginx --variant=api

nginx-build-loris: .docker/_build_deps
	./scripts/build_docker_image.py --project=nginx --variant=loris

nginx-build-services: .docker/_build_deps
	./scripts/build_docker_image.py --project=nginx --variant=services

nginx-build-grafana: .docker/_build_deps
	./scripts/build_docker_image.py --project=nginx --variant=grafana

## Build images for all of our nginx proxies
nginx-build:	\
	nginx-build-api \
	nginx-build-loris \
	nginx-build-services \
    nginx-build-grafana



nginx-deploy-api: nginx-build-api
	./scripts/deploy_docker_to_aws.py --project=nginx_api --infra-bucket=$(INFRA_BUCKET)

nginx-deploy-loris: nginx-build-loris
	./scripts/deploy_docker_to_aws.py --project=nginx_loris --infra-bucket=$(INFRA_BUCKET)

nginx-deploy-services: nginx-build-services
	./scripts/deploy_docker_to_aws.py --project=nginx_services --infra-bucket=$(INFRA_BUCKET)

nginx-deploy-grafana: nginx-build-grafana
	./scripts/deploy_docker_to_aws.py --project=nginx_grafana --infra-bucket=$(INFRA_BUCKET)

## Push images for all of our nginx proxies
nginx-deploy:	\
	nginx-deploy-api \
	nginx-deploy-loris \
	nginx-deploy-services \
	nginx-deploy-grafana



sbt-test-common:
	sbt 'project common' ';dockerComposeUp;test;dockerComposeStop'

sbt-test-api:
	sbt 'project api' ';dockerComposeUp;test;dockerComposeStop'

sbt-test-id_minter:
	sbt 'project id_minter' ';dockerComposeUp;test;dockerComposeStop'

sbt-test-ingestor:
	sbt 'project ingestor' ';dockerComposeUp;test;dockerComposeStop'

sbt-test-miro_adapter:
	sbt 'project miro_adapter' ';dockerComposeUp;test;dockerComposeStop'

sbt-test-reindexer:
	sbt 'project reindexer' ';dockerComposeUp;test;dockerComposeStop'

sbt-test-transformer:
	sbt 'project transformer' ';dockerComposeUp;test;dockerComposeStop'

sbt-test: \
	sbt-test-api	\
	sbt-test-id_minter \
	sbt-test-ingestor   \
	sbt-test-miro_adapter \
	sbt-test-reindexer	\
	sbt-test-transformer



sbt-build-api: .docker/_build_deps sbt-test-api
	./scripts/build_sbt_image.py --project=api

sbt-build-id_minter: .docker/_build_deps sbt-test-id_minter
	./scripts/build_sbt_image.py --project=id_minter

sbt-build-ingestor: .docker/_build_deps sbt-test-ingestor
	./scripts/build_sbt_image.py --project=ingestor

sbt-build-miro_adapter: .docker/_build_deps sbt-test-miro_adapter
	./scripts/build_sbt_image.py --project=miro_adapter

sbt-build-reindexer: .docker/_build_deps sbt-test-reindexer
	./scripts/build_sbt_image.py --project=reindexer

sbt-build-transformer: .docker/_build_deps sbt-test-transformer
	./scripts/build_sbt_image.py --project=transformer

sbt-build: \
	sbt-build-api	\
	sbt-build-id_minter \
	sbt-build-ingestor   \
	sbt-build-miro_adapter \
	sbt-build-reindexer	\
	sbt-build-transformer



sbt-deploy-api: sbt-build-api
	./scripts/deploy_docker_to_aws.py --project=api --infra-bucket=$(INFRA_BUCKET)

sbt-deploy-id_minter: sbt-build-id_minter
	./scripts/deploy_docker_to_aws.py --project=id_minter --infra-bucket=$(INFRA_BUCKET)

sbt-deploy-ingestor: sbt-build-ingestor
	./scripts/deploy_docker_to_aws.py --project=ingestor --infra-bucket=$(INFRA_BUCKET)

sbt-deploy-miro_adapter: sbt-build-miro_adapter
	./scripts/deploy_docker_to_aws.py --project=miro_adapter --infra-bucket=$(INFRA_BUCKET)

sbt-deploy-reindexer: sbt-build-reindexer
	./scripts/deploy_docker_to_aws.py --project=reindexer --infra-bucket=$(INFRA_BUCKET)

sbt-deploy-transformer: sbt-build-transformer
	./scripts/deploy_docker_to_aws.py --project=transformer --infra-bucket=$(INFRA_BUCKET)

sbt-deploy: \
	sbt-deploy-api	\
	sbt-deploy-id_minter \
	sbt-deploy-ingestor   \
	sbt-deploy-miro_adapter \
	sbt-deploy-reindexer	\
	sbt-deploy-transformer



# Tasks for running terraform #

.docker/lambda_deps: .docker/python3.6_ci
	docker run -v $$(pwd)/lambdas:/data -e OP=install-deps python3.6_ci:latest

## Build ami
packer-build: .docker/packer_ci
	docker run -v $$HOME/.aws:/root/.aws -v $$(pwd)/packer:/data packer_ci:latest

## Run a plan
terraform-plan: .docker/terraform_ci .docker/lambda_deps
	docker run -v $$(pwd):/data -v $$HOME/.aws:/root/.aws -v $$HOME/.ssh:/root/.ssh -e OP=plan terraform_ci:latest

## Run an apply
terraform-apply: .docker/terraform_ci
	docker run -v $$(pwd):/data -v $$HOME/.aws:/root/.aws -v $$HOME/.ssh:/root/.ssh -e OP=apply terraform_ci:latest

# Tasks for running linting #

## Run JSON linting over the ontologies directory
lint-ontologies: .docker/jslint_ci
	docker run -v $$(pwd)/ontologies:/data jslint_ci:latest

## Run flake8 linting over our Python code
lint-python: .docker/python3.6_ci
	docker run -v $$(pwd):/data -e OP=lint python3.6_ci:latest

## Run tests for our Lambda code
test-lambdas: .docker/python3.6_ci
	./scripts/run_docker_with_aws_credentials.sh -v $$(pwd)/lambdas:/data -e OP=test python3.6_ci:latest

format-terraform: .docker/terraform_ci
	docker run -v $$(pwd):/data -e OP=fmt terraform_ci

format-scala:
	sbt scalafmt

format: \
	format-terraform \
	format-scala

check-format: format
	git diff --exit-code


.PHONY: clean help

## Display this help text
help: # Some kind of magic from https://gist.github.com/rcmachado/af3db315e31383502660
	$(info Available targets)
	@awk '/^[a-zA-Z\-\_0-9\/]+:/ {                                      \
		nb = sub( /^## /, "", helpMsg );                                \
		if(nb == 0) {                                                   \
			helpMsg = $$0;                                              \
			nb = sub( /^[^:]*:.* ## /, "", helpMsg );                   \
		}                                                               \
		if (nb)                                                         \
			printf "\033[1;31m%-" width "s\033[0m %s\n", $$1, helpMsg;  \
	}                                                                   \
	{ helpMsg = $$0 }'                                                  \
	width=30                                                            \
	$(MAKEFILE_LIST)
