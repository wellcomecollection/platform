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

nginx-build-api-delta:
	$(call nginx_build_image,api-delta)

nginx-build-loris:
	$(call nginx_build_image,loris)

nginx-build-loris-delta:
	$(call nginx_build_image,loris-delta)

nginx-build-grafana:
	$(call nginx_build_image,grafana)

nginx-build: nginx-build-api-delta nginx-build-loris nginx-build-loris-delta nginx-build-grafana

nginx-publish-api-delta: nginx-build-api-delta
	$(call publish_service,nginx_api-delta)

nginx-publish-loris: nginx-build-loris
	$(call publish_service,nginx_loris)

nginx-publish-loris-delta: nginx-build-loris-delta
	$(call publish_service,nginx_loris-delta)

nginx-publish-grafana: nginx-build-grafana
	$(call publish_service,nginx_grafana)

nginx-publish: nginx-publish-api-delta nginx-publish-loris nginx-publish-loris-delta nginx-publish-grafana
