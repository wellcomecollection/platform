export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra
export INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

nginx-build-api:
	$(call nginx_build_image,api)

nginx-build-loris:
	$(call nginx_build_image,loris)

nginx-build-services:
	$(call nginx_build_image,services)

nginx-build-grafana:
	$(call nginx_build_image,grafana)

nginx-build: nginx-build-api nginx-build-loris nginx-build-services nginx-build-grafana



nginx-deploy-api: nginx-build-api
	$(call publish_service,nginx_api)

nginx-deploy-loris: nginx-build-loris
	$(call publish_service,nginx_loris)

nginx-deploy-services: nginx-build-services
	$(call publish_service,nginx_services)

nginx-deploy-grafana: nginx-build-grafana
	$(call publish_service,nginx_grafana)

nginx-deploy: nginx-deploy-api nginx-deploy-loris nginx-deploy-services nginx-deploy-grafana