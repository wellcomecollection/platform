export ECR_BASE_URI = 760097843905.dkr.ecr.eu-west-1.amazonaws.com/uk.ac.wellcome
export REGISTRY_ID  = 760097843905

ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/makefiles/functions.Makefile


nginx_apigw-build:
	$(call build_image,nginx_apigw,nginx/apigw.Dockerfile)

nginx_loris-build:
	$(call build_image,nginx_loris,nginx/loris.Dockerfile)

nginx_grafana-build:
	$(call build_image,nginx_grafana,nginx/grafana.Dockerfile)

nginx-build:    nginx_apigw-build \
                nginx_loris-build \
                nginx_grafana-build

nginx_apigw-publish: nginx_apigw-build
	$(call publish_service_ssm,nginx_apigw,nginx,$(ECR_BASE_URI),$(REGISTRY_ID))

nginx_loris-publish: nginx_loris-build
	$(call publish_service_ssm,nginx_loris,nginx,$(ECR_BASE_URI),$(REGISTRY_ID))

nginx_grafana-publish: nginx_grafana-build
	$(call publish_service_ssm,nginx_grafana,nginx,$(ECR_BASE_URI),$(REGISTRY_ID))

nginx-publish:  nginx_apigw-publish \
                nginx_loris-publish \
                nginx_grafana-publish
