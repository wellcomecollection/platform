export INFRA_BUCKET = platform-infra


# Build the Docker images used for CI tasks.
#
# The script sticks a record that the image has been built in .docker, so the
# image isn't rebuilt unless you run 'make clean' first.  This makes CI tasks
# slightly less chatty when run locally.

clean:
	rm -rf .docker

.docker/jslint_ci:
	./scripts/build_ci_docker_image.py --project=jslint_ci --dir=docker/jslint_ci

.docker/python3.6_ci:
	./scripts/build_ci_docker_image.py --project=python3.6_ci --dir=docker/python3.6_ci

.docker/terraform_ci:
	./scripts/build_ci_docker_image.py --project=terraform_ci --dir=docker/terraform_ci

.docker/_build_deps:
	pip3 install --upgrade boto3 docopt
	mkdir -p .docker && touch .docker/_build_deps

.docker/image_builder:
	./scripts/build_ci_docker_image.py \
		--project=image_builder \
		--dir=builds \
		--file=builds/image_builder.Dockerfile

.docker/publish_service_to_aws:
	./scripts/build_ci_docker_image.py \
		--project=publish_service_to_aws \
		--dir=builds \
		--file=builds/publish_service_to_aws.Dockerfile

.docker/miro_adapter_tests:
	./scripts/build_ci_docker_image.py \
		--project=miro_adapter_tests \
		--dir=miro_adapter \
		--file=miro_adapter/miro_adapter_tests.Dockerfile


## Build the image for gatling
gatling-build: .docker/image_builder
	docker run -v /var/run/docker.sock:/var/run/docker.sock -v $$(pwd):/repo image_builder --project=gatling

## Deploy the image for gatling
gatling-deploy: gatling-build .docker/publish_service_to_aws
	PROJECT=gatling ./builds/publish_service.sh

## Build the image for the cache cleaner
cache_cleaner-build: .docker/image_builder
	docker run -v /var/run/docker.sock:/var/run/docker.sock -v $$(pwd):/repo image_builder --project=cache_cleaner

## Deploy the image for the cache cleaner
cache_cleaner-deploy: cache_cleaner-build .docker/publish_service_to_aws
	PROJECT=cache_cleaner ./builds/publish_service.sh


## Build the image for tif-metadata
tif-metadata-build: .docker/image_builder
	docker run -v /var/run/docker.sock:/var/run/docker.sock -v $$(pwd):/repo image_builder --project=tif-metadata

## Deploy the image for tif-metadata
tif-metadata-deploy: tif-metadata-build .docker/publish_service_to_aws
	PROJECT=tif-metadata ./builds/publish_service.sh


## Build the image for Loris
loris-build: .docker/image_builder
	docker run -v /var/run/docker.sock:/var/run/docker.sock -v $$(pwd):/repo image_builder --project=loris

## Deploy the image for Loris
loris-deploy: loris-build .docker/publish_service_to_aws
	PROJECT=loris ./builds/publish_service.sh


miro_adapter-build: .docker/image_builder
	docker run -v /var/run/docker.sock:/var/run/docker.sock -v $$(pwd):/repo image_builder --project=miro_adapter --file=miro_adapter/Dockerfile

miro_adapter-test: miro_adapter-build .docker/miro_adapter_tests
	rm -rf $$(pwd)/miro_adapter/__pycache__
	rm -rf $$(pwd)/miro_adapter/*.pyc
	docker run -v $$(pwd)/miro_adapter:/miro_adapter miro_adapter_tests

miro_adapter-deploy: miro_adapter-build .docker/publish_service_to_aws
	PROJECT=miro_adapter ./builds/publish_service.sh


elasticdump-build: .docker/image_builder
	docker run -v /var/run/docker.sock:/var/run/docker.sock -v $$(pwd):/repo image_builder --project=elasticdump

elasticdump-deploy: elasticdump-build .docker/publish_service_to_aws
	PROJECT=elasticdump ./builds/publish_service.sh


api_docs-build: .docker/_build_deps
	docker run -v /var/run/docker.sock:/var/run/docker.sock -v $$(pwd):/repo image_builder --project=update_api_docs

api_docs-deploy: api_docs-build .docker/publish_service_to_aws
	PROJECT=api_docs ./builds/publish_service.sh


nginx-build-api: .docker/image_builder
	docker run -v /var/run/docker.sock:/var/run/docker.sock -v $$(pwd):/repo image_builder --project=nginx --variant=api

nginx-build-loris: .docker/image_builder
	docker run -v /var/run/docker.sock:/var/run/docker.sock -v $$(pwd):/repo image_builder --project=nginx --variant=loris

nginx-build-services: .docker/image_builder
	docker run -v /var/run/docker.sock:/var/run/docker.sock -v $$(pwd):/repo image_builder --project=nginx --variant=services

nginx-build-grafana: .docker/image_builder
	docker run -v /var/run/docker.sock:/var/run/docker.sock -v $$(pwd):/repo image_builder --project=nginx --variant=grafana

## Build images for all of our nginx proxies
nginx-build:	\
	nginx-build-api \
	nginx-build-loris \
	nginx-build-services \
    nginx-build-grafana



nginx-deploy-api: nginx-build-api .docker/publish_service_to_aws
	PROJECT=nginx_api ./builds/publish_service.sh

nginx-deploy-loris: nginx-build-loris .docker/publish_service_to_aws
	PROJECT=nginx_loris ./builds/publish_service.sh

nginx-deploy-services: nginx-build-services .docker/publish_service_to_aws
	PROJECT=nginx_services ./builds/publish_service.sh

nginx-deploy-grafana: nginx-build-grafana .docker/publish_service_to_aws
	PROJECT=nginx_grafana ./builds/publish_service.sh

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



sbt-build-api: .docker/_build_deps
	./scripts/build_sbt_image.py --project=api

sbt-build-id_minter: .docker/_build_deps
	./scripts/build_sbt_image.py --project=id_minter

sbt-build-ingestor: .docker/_build_deps
	./scripts/build_sbt_image.py --project=ingestor

sbt-build-miro_adapter: .docker/_build_deps
	./scripts/build_sbt_image.py --project=miro_adapter

sbt-build-reindexer: .docker/_build_deps
	./scripts/build_sbt_image.py --project=reindexer

sbt-build-transformer: .docker/_build_deps
	./scripts/build_sbt_image.py --project=transformer

sbt-build: \
	sbt-build-api	\
	sbt-build-id_minter \
	sbt-build-ingestor   \
	sbt-build-miro_adapter \
	sbt-build-reindexer	\
	sbt-build-transformer



sbt-deploy-api: sbt-build-api .docker/publish_service_to_aws
	PROJECT=api ./builds/publish_service.sh

sbt-deploy-id_minter: sbt-build-id_minter .docker/publish_service_to_aws
	PROJECT=id_minter ./builds/publish_service.sh

sbt-deploy-ingestor: sbt-build-ingestor .docker/publish_service_to_aws
	PROJECT=ingestor ./builds/publish_service.sh

sbt-deploy-reindexer: sbt-build-reindexer .docker/publish_service_to_aws
	PROJECT=reindexer ./builds/publish_service.sh

sbt-deploy-transformer: sbt-build-transformer .docker/publish_service_to_aws
	PROJECT=transformer ./builds/publish_service.sh

sbt-deploy: \
	sbt-deploy-api	\
	sbt-deploy-id_minter \
	sbt-deploy-ingestor   \
	sbt-deploy-reindexer	\
	sbt-deploy-transformer



# Tasks for running terraform #

.docker/_lambda_deps: .docker/python3.6_ci
	docker run -v $$(pwd)/lambdas:/data -e OP=install-deps python3.6_ci:latest
	mkdir -p .docker && touch .docker/_lambda_deps

## Run a plan
terraform-plan: .docker/terraform_ci .docker/_lambda_deps
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
	docker run -v $$(pwd):/data -v $$HOME/.aws:/root/.aws -e OP=fmt terraform_ci

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
