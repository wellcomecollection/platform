# Tasks for building Docker images #

## Build the image used for jslint
docker-build-jslint:
	docker build ./docker/jslint_ci --tag jslint_ci

## Build the image used for flake8 linting
docker-build-flake8:
	docker build ./docker/python3.6_ci --tag python3.6_ci

## Build the images for the nginx proxies
docker-build-nginx:
	./docker/nginx/manage_images.sh BUILD

## Build the image for terraform
docker-build-terraform:
	docker build ./docker/terraform_ci --tag terraform_ci

# Tasks for pushing images to ECR #
install-docker-build-deps:
	pip3 install --upgrade boto3 docker docopt

nginx-build-api: install-docker-build-deps
	./scripts/build_nginx_image.py --variant=api

nginx-build-loris: install-docker-build-deps
	./scripts/build_nginx_image.py --variant=loris

nginx-build-services: install-docker-build-deps
	./scripts/build_nginx_image.py --variant=services

## Build images for all of our nginx proxies
nginx-build:	\
	nginx-build-api \
	nginx-build-loris \
	nginx-build-services



nginx-deploy-api: nginx-build-api
	./scripts/deploy_docker_to_aws.py --project=nginx_api --infra-bucket=$(INFRA_BUCKET)

nginx-deploy-loris: nginx-build-loris
	./scripts/deploy_docker_to_aws.py --project=nginx_loris --infra-bucket=$(INFRA_BUCKET)

nginx-deploy-services: nginx-build-services
	./scripts/deploy_docker_to_aws.py --project=nginx_services --infra-bucket=$(INFRA_BUCKET)

## Push images for all of our nginx proxies
nginx-deploy:	\
	nginx-deploy-api \
	nginx-deploy-loris \
	nginx-deploy-services

## Push images for the nginx proxies to ECR
docker-deploy-nginx:
	./docker/nginx/manage_images.sh DEPLOY

sbt-test-common:
	sbt 'project common' ';dockerComposeUp;test;dockerComposeStop'

sbt-test-api:
	sbt 'project common' ';dockerComposeUp;test;dockerComposeStop'

sbt-test-calm_adapter:
	sbt 'project calm_adapter' ';dockerComposeUp;test;dockerComposeStop'

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
	sbt-test-calm_adapter	\
	sbt-test-id_minter \
	sbt-test-ingestor   \
	sbt-test-miro_adapter \
	sbt-test-reindexer	\
	sbt-test-transformer



sbt-build-api: install-docker-build-deps
	./scripts/build_sbt_image.py --project=api

sbt-build-calm_adapter: install-docker-build-deps
	./scripts/build_sbt_image.py --project=calm_adapter

sbt-build-id_minter: install-docker-build-deps
	./scripts/build_sbt_image.py --project=id_minter

sbt-build-ingestor: install-docker-build-deps
	./scripts/build_sbt_image.py --project=ingestor

sbt-build-miro_adapter: install-docker-build-deps
	./scripts/build_sbt_image.py --project=miro_adapter

sbt-build-reindexer: install-docker-build-deps
	./scripts/build_sbt_image.py --project=reindexer

sbt-build-transformer: install-docker-build-deps
	./scripts/build_sbt_image.py --project=reindexer

sbt-build: \
	sbt-build-api	\
	sbt-build-calm_adapter	\
	sbt-build-id_minter \
	sbt-build-ingestor   \
	sbt-build-miro_adapter \
	sbt-build-reindexer	\
	sbt-build-transformer



sbt-deploy-api: sbt-build-api
	./scripts/deploy_docker_to_aws.py --project=api --infra-bucket=$(INFRA_BUCKET)

sbt-deploy-calm_adapter: sbt-build-calm_adapter
	./scripts/deploy_docker_to_aws.py --project=calm_adapter --infra-bucket=$(INFRA_BUCKET)

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
	sbt-deploy-calm_adapter	\
	sbt-deploy-id_minter \
	sbt-deploy-ingestor   \
	sbt-deploy-miro_adapter \
	sbt-deploy-reindexer	\
	sbt-deploy-transformer



# Tasks for running linting #

## Run JSON linting over the ontologies directory
lint-ontologies: docker-build-jslint
	docker run -v $$(pwd)/ontologies:/data jslint_ci:latest

## Run flake8 linting over our Lambda code
lint-lambdas: docker-build-flake8
	docker run -v $$(pwd)/lambdas:/data python3.6_ci:latest

# Tasks for running terraform #

## Run a plan
terraform-plan: docker-build-terraform
	docker run -v $$(pwd):/data -v $$HOME/.aws:/root/.aws -v $$HOME/.ssh:/root/.ssh terraform_ci:latest

## Run an apply
terraform-apply: docker-build-terraform
		docker run -v $$(pwd):/data -v $$HOME/.aws:/root/.aws -v $$HOME/.ssh:/root/.ssh -e OP=apply terraform_ci:latest


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
	width=20                                                            \
	$(MAKEFILE_LIST)
