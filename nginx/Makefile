export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

# Build and tag a Docker image (specifically for ngnix).
#
# Args:
#   $1 - Name of the variant.
#
define nginx_build_image
	$(ROOT)/builds/docker_run.py \
		--dind -- \
		wellcome/image_builder:latest \
            --project=nginx \
            --variant=$(1)
endef

nginx-build-api:
	$(call nginx_build_image,api)

nginx-build-loris:
	$(call nginx_build_image,loris)

nginx-build-grafana:
	$(call nginx_build_image,grafana)

nginx-build: nginx-build-api nginx-build-loris nginx-build-grafana



nginx-deploy-api: nginx-build-api
	$(call publish_service,nginx_api)

nginx-deploy-loris: nginx-build-loris
	$(call publish_service,nginx_loris)

nginx-deploy-grafana: nginx-build-grafana
	$(call publish_service,nginx_grafana)

nginx-deploy: nginx-deploy-api nginx-deploy-loris nginx-deploy-grafana