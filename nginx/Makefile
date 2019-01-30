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
		wellcome/image_builder:23 \
            --project=nginx \
            --variant=$(1) \
            --file=nginx/$(2)
endef

nginx-build-api-gw:
	$(call nginx_build_image,api-gw,api-gw.Dockerfile.template)

nginx-build-loris:
	$(call nginx_build_image,loris,Dockerfile)

nginx-build-services:
	$(call nginx_build_image,services,services.Dockerfile.template)

nginx-build-loris-delta:
	$(call nginx_build_image,loris-delta,Dockerfile)

nginx-build-grafana:
	$(call nginx_build_image,grafana,Dockerfile)

nginx-build: nginx-build-loris nginx-build-services nginx-build-loris-delta nginx-build-grafana nginx-build-api-gw

nginx-publish-api-gw: nginx-build-api-gw
	$(call publish_service_ssm,nginx_api-gw,catalogue_api)

nginx-publish-loris: nginx-build-loris
	$(call publish_service,nginx_loris)

nginx-publish-services: nginx-build-services
	$(call publish_service,nginx_services)

nginx-publish-loris-delta: nginx-build-loris-delta
	$(call publish_service,nginx_loris-delta)

nginx-publish-grafana: nginx-build-grafana
	$(call publish_service,nginx_grafana)

nginx-publish: nginx-publish-loris nginx-publish-services nginx-publish-loris-delta nginx-publish-grafana nginx-publish-api-gw
