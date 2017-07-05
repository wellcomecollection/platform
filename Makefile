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

## Push images for the nginx proxies to ECR
docker-deploy-nginx:
	./docker/nginx/manage_images.sh DEPLOY

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
