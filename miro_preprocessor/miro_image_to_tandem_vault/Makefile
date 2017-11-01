ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

miro_tandem_vault_uploader-build: $(ROOT)/.docker/image_builder
	$(ROOT)/builds/docker_run.py --dind -- \
		image_builder:latest \
		--project=tandem_vault_uploader \
		--file=miro_preprocessor/miro_image_to_tandem_vault/tandem_vault_uploader.Dockerfile

miro_tandem_vault_uploader-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT)/miro_preprocessor/miro_image_to_tandem_vault/src:/data \
		--env OP=test \
		python3.6_ci:latest

miro_tandem_vault_uploader-publish: $(ROOT)/.docker/publish_service_to_aws
	$(ROOT)/builds/docker_run.py --aws --dind -- \
		publish_service_to_aws:latest \
		--project=tandem_vault_uploader \
		--infra-bucket=platform-infra

miro_tandem_vault_enricher-build: $(ROOT)/.docker/image_builder
	$(ROOT)/builds/docker_run.py --dind -- \
		image_builder:latest \
		--project=tandem_vault_enricher \
		--file=miro_preprocessor/miro_image_to_tandem_vault/tandem_vault_enricher.Dockerfile

miro_tandem_vault_enricher-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT)/miro_preprocessor/miro_image_to_tandem_vault/src:/data \
		--env OP=test \
		python3.6_ci:latest

miro_tandem_vault_enricher-publish: $(ROOT)/.docker/publish_service_to_aws
	$(ROOT)/builds/docker_run.py --aws --dind -- \
		publish_service_to_aws:latest \
		--project=tandem_vault_enricher \
		--infra-bucket=platform-infra