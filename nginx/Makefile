ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile


# Build and tag a Docker image (specifically for nginx).
#
# Args:
#   $1 - Name of the variant.
#
define nginx_build_image
	$(ROOT)/docker_run.py \
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


nginx-publish-api: nginx-build-api
	$(call publish_service,nginx_api)

nginx-publish-loris: nginx-build-loris
	$(call publish_service,nginx_loris)

nginx-publish-grafana: nginx-build-grafana
	$(call publish_service,nginx_grafana)

nginx-publish: nginx-publish-api nginx-publish-loris nginx-publish-grafana
