INFRA_BUCKET = platform-infra


## Build the image used for jslint
docker-build-jslint:
	docker build ./docker/jslint_ci --tag jslint_ci

## Build the image used for Python 3.6 work
docker-build-python36:
	docker build ./docker/python3.6_ci --tag python3.6_ci

## Build the image for terraform
docker-build-terraform:
	docker build ./docker/terraform_ci --tag terraform_ci

## Build the image for gatling
docker-build-gatling:
	docker build ./docker/gatling --tag gatling_ci

## Build the image for the cache cleaner
docker-build-cache_cleaner: install-docker-build-deps
	docker build ./efs_cache_cleaner --tag cache_cleaner

## Build the image for the cache cleaner
docker-deploy-cache_cleaner: docker-build-cache_cleaner
	./scripts/deploy_docker_to_aws.py --project=cache_cleaner --infra-bucket=$(INFRA_BUCKET)

## Build the image for scala in CI
docker-build-scala_ci:
	docker build ./docker/scala_ci --tag scala_ci


install-docker-build-deps:
	pip3 install --upgrade boto3 docker docopt

nginx-build-api: install-docker-build-deps
	./scripts/build_nginx_image.py --variant=api

nginx-build-loris: install-docker-build-deps
	./scripts/build_nginx_image.py --variant=loris

nginx-build-services: install-docker-build-deps
	./scripts/build_nginx_image.py --variant=services

nginx-build-grafana: install-docker-build-deps
	./scripts/build_nginx_image.py --variant=grafana

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



sbt-test-common: docker-build-scala_ci
	./scripts/scala_ci_docker.sh common test $(INFRA_BUCKET)

sbt-test-api: docker-build-scala_ci
	./scripts/scala_ci_docker.sh api test $(INFRA_BUCKET)

sbt-test-id_minter: docker-build-scala_ci
	./scripts/scala_ci_docker.sh id_minter test $(INFRA_BUCKET)

sbt-test-ingestor: docker-build-scala_ci
	./scripts/scala_ci_docker.sh ingestor test $(INFRA_BUCKET)

sbt-test-miro_adapter: docker-build-scala_ci
	./scripts/scala_ci_docker.sh miro_adapter test $(INFRA_BUCKET)

sbt-test-reindexer: docker-build-scala_ci
	./scripts/scala_ci_docker.sh reindexer test $(INFRA_BUCKET)

sbt-test-transformer: docker-build-scala_ci
	./scripts/scala_ci_docker.sh transformer test $(INFRA_BUCKET)

sbt-test: \
	sbt-test-api	\
	sbt-test-id_minter \
	sbt-test-ingestor   \
	sbt-test-miro_adapter \
	sbt-test-reindexer	\
	sbt-test-transformer



sbt-build-api: docker-build-scala_ci
	./scripts/scala_ci_docker.sh api build $(INFRA_BUCKET)

sbt-build-id_minter: docker-build-scala_ci
	./scripts/scala_ci_docker.sh id_minter build $(INFRA_BUCKET)

sbt-build-ingestor: docker-build-scala_ci
	./scripts/scala_ci_docker.sh ingestor build $(INFRA_BUCKET)

sbt-build-miro_adapter: docker-build-scala_ci
	./scripts/scala_ci_docker.sh miro_adapter build $(INFRA_BUCKET)

sbt-build-reindexer: docker-build-scala_ci
	./scripts/scala_ci_docker.sh reindexer build $(INFRA_BUCKET)

sbt-build-transformer: docker-build-scala_ci
	./scripts/scala_ci_docker.sh transformer build $(INFRA_BUCKET)

sbt-build: \
	sbt-build-api	\
	sbt-build-id_minter \
	sbt-build-ingestor   \
	sbt-build-miro_adapter \
	sbt-build-reindexer	\
	sbt-build-transformer



sbt-deploy-api: sbt-build-api
	./scripts/scala_ci_docker.sh api deploy $(INFRA_BUCKET)

sbt-deploy-id_minter: sbt-build-id_minter
	./scripts/scala_ci_docker.sh id_minter deploy $(INFRA_BUCKET)

sbt-deploy-ingestor: sbt-build-ingestor
	./scripts/scala_ci_docker.sh ingestor deploy $(INFRA_BUCKET)

sbt-deploy-miro_adapter: sbt-build-miro_adapter
	./scripts/scala_ci_docker.sh miro_adapter deploy $(INFRA_BUCKET)

sbt-deploy-reindexer: sbt-build-reindexer
	./scripts/scala_ci_docker.sh reindexer deploy $(INFRA_BUCKET)

sbt-deploy-transformer: sbt-build-transformer
	./scripts/scala_ci_docker.sh transformer deploy $(INFRA_BUCKET)

sbt-deploy: \
	sbt-deploy-api	\
	sbt-deploy-id_minter \
	sbt-deploy-ingestor   \
	sbt-deploy-miro_adapter \
	sbt-deploy-reindexer	\
	sbt-deploy-transformer



# Tasks for running terraform #

install-lambda-deps: docker-build-python36
	docker run -v $$(pwd)/lambdas:/data -e OP=install-deps python3.6_ci:latest

## Run a plan
terraform-plan: docker-build-terraform install-lambda-deps
	docker run -v $$(pwd):/data -v $$HOME/.aws:/root/.aws -v $$HOME/.ssh:/root/.ssh terraform_ci:latest

## Run an apply
terraform-apply: docker-build-terraform
		docker run -v $$(pwd):/data -v $$HOME/.aws:/root/.aws -v $$HOME/.ssh:/root/.ssh -e OP=apply terraform_ci:latest



# Tasks for running linting #

## Run JSON linting over the ontologies directory
lint-ontologies: docker-build-jslint
	docker run -v $$(pwd)/ontologies:/data jslint_ci:latest

## Run flake8 linting over our Lambda code
lint-lambdas: docker-build-python36
	docker run -v $$(pwd)/lambdas:/data -e OP=lint python3.6_ci:latest

## Run tests for our Lambda code
test-lambdas: docker-build-python36
	./scripts/run_docker_with_aws_credentials.sh -v $$(pwd)/lambdas:/data -e OP=test python3.6_ci:latest

format-terraform:
	terraform fmt

format-scala:
	sbt scalafmt

format: \
	format-terraform \
	format-scala

check-format: format
	git diff --exit-code


# Tasks for running gatling #


## Run JSON linting over the ontologies directory
gatling-loris: docker-build-gatling
	docker run \
		-v $$(pwd)/gatling/user-files:/opt/gatling/user-files \
		-v $$(pwd)/gatling/results:/opt/gatling/results \
		-v $$(pwd)/gatling/data:/opt/gatling/data \
		-e SIMULATION=testing.load.LorisSimulation \
		gatling_ci:latest

.PHONY: help

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
