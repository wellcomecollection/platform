include shared.Makefile
include loris/Makefile
include lambdas/Makefile
include shared_infra/Makefile
include miro_preprocessor/Makefile
include monitoring/Makefile
include ontologies/Makefile


## Build the image for gatling
gatling-build: $(ROOT)/.docker/image_builder
	./builds/docker_run.py --dind -- image_builder --project=gatling

## Deploy the image for gatling
gatling-deploy: gatling-build $(ROOT)/.docker/publish_service_to_aws
	PROJECT=gatling ./builds/publish_service.sh

## Build the image for the cache cleaner
cache_cleaner-build: $(ROOT)/.docker/image_builder
	./builds/docker_run.py --dind -- image_builder --project=cache_cleaner

## Deploy the image for the cache cleaner
cache_cleaner-deploy: cache_cleaner-build $(ROOT)/.docker/publish_service_to_aws
	PROJECT=cache_cleaner ./builds/publish_service.sh


## Build the image for tif-metadata
tif-metadata-build: $(ROOT)/.docker/image_builder
	./builds/docker_run.py --dind -- image_builder --project=tif-metadata

## Deploy the image for tif-metadata
tif-metadata-deploy: tif-metadata-build $(ROOT)/.docker/publish_service_to_aws
	PROJECT=tif-metadata ./builds/publish_service.sh


elasticdump-build: $(ROOT)/.docker/image_builder
	./builds/docker_run.py --dind -- image_builder --project=elasticdump

elasticdump-deploy: elasticdump-build $(ROOT)/.docker/publish_service_to_aws
	PROJECT=elasticdump ./builds/publish_service.sh

api_docs-build: $(ROOT)/.docker/image_builder
	./builds/docker_run.py --dind -- image_builder --project=update_api_docs

api_docs-deploy: api_docs-build $(ROOT)/.docker/publish_service_to_aws
	PROJECT=update_api_docs ./builds/publish_service.sh


nginx-build-api: $(ROOT)/.docker/image_builder
	./builds/docker_run.py --dind -- image_builder --project=nginx --variant=api

nginx-build-loris: $(ROOT)/.docker/image_builder
	./builds/docker_run.py --dind -- image_builder --project=nginx --variant=loris

nginx-build-services: $(ROOT)/.docker/image_builder
	./builds/docker_run.py --dind -- image_builder --project=nginx --variant=services

nginx-build-grafana: $(ROOT)/.docker/image_builder
	./builds/docker_run.py --dind -- image_builder --project=nginx --variant=grafana

## Build images for all of our nginx proxies
nginx-build:	\
	nginx-build-api \
	nginx-build-loris \
	nginx-build-services \
    nginx-build-grafana



nginx-deploy-api: nginx-build-api $(ROOT)/.docker/publish_service_to_aws
	PROJECT=nginx_api ./builds/publish_service.sh

nginx-deploy-loris: nginx-build-loris $(ROOT)/.docker/publish_service_to_aws
	PROJECT=nginx_loris ./builds/publish_service.sh

nginx-deploy-services: nginx-build-services $(ROOT)/.docker/publish_service_to_aws
	PROJECT=nginx_services ./builds/publish_service.sh

nginx-deploy-grafana: nginx-build-grafana $(ROOT)/.docker/publish_service_to_aws
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

sbt-test-reindexer:
	sbt 'project reindexer' ';dockerComposeUp;test;dockerComposeStop'

sbt-test-transformer:
	sbt 'project transformer' ';dockerComposeUp;test;dockerComposeStop'

sbt-test: \
	sbt-test-api	\
	sbt-test-id_minter \
	sbt-test-ingestor   \
	sbt-test-reindexer	\
	sbt-test-transformer


.docker/sbt_image_builder:
	./builds/build_ci_docker_image.py \
		--project=sbt_image_builder \
		--dir=builds \
		--file=builds/sbt_image_builder.Dockerfile

sbt-build-api: .docker/sbt_image_builder
	PROJECT=api ./builds/run_sbt_image_build.sh

sbt-build-id_minter: .docker/sbt_image_builder
	PROJECT=id_minter ./builds/run_sbt_image_build.sh

sbt-build-ingestor: .docker/sbt_image_builder
	PROJECT=ingestor ./builds/run_sbt_image_build.sh

sbt-build-reindexer: .docker/sbt_image_builder
	PROJECT=reindexer ./builds/run_sbt_image_build.sh

sbt-build-transformer: .docker/sbt_image_builder
	PROJECT=transformer ./builds/run_sbt_image_build.sh

sbt-build: \
	sbt-build-api	\
	sbt-build-id_minter \
	sbt-build-ingestor   \
	sbt-build-reindexer	\
	sbt-build-transformer



sbt-deploy-api: sbt-build-api $(ROOT)/.docker/publish_service_to_aws
	PROJECT=api ./builds/publish_service.sh

sbt-deploy-id_minter: sbt-build-id_minter $(ROOT)/.docker/publish_service_to_aws
	PROJECT=id_minter ./builds/publish_service.sh

sbt-deploy-ingestor: sbt-build-ingestor $(ROOT)/.docker/publish_service_to_aws
	PROJECT=ingestor ./builds/publish_service.sh

sbt-deploy-reindexer: sbt-build-reindexer $(ROOT)/.docker/publish_service_to_aws
	PROJECT=reindexer ./builds/publish_service.sh

sbt-deploy-transformer: sbt-build-transformer $(ROOT)/.docker/publish_service_to_aws
	PROJECT=transformer ./builds/publish_service.sh


format: \
	format-terraform \
	format-scala

check-format: format lint-python lint-ontologies
	git diff --exit-code


.PHONY: help format check-format

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
