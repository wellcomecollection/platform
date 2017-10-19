ROOT = $(shell git rev-parse --show-toplevel)
INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

xml_to_json_converter-build: xml_to_json_converter-test $(ROOT)/.docker/image_builder
	$(ROOT)/builds/docker_run.py --dind -- \
		-v $(ROOT):/repo \
		image_builder:latest \
		--project=xml_to_json_converter \
		--file=miro_preprocessor/xml_to_json_converter/Dockerfile

xml_to_json_converter-test: $(ROOT)/.docker/python3.6_ci
	docker run \
		-v $(ROOT)/miro_preprocessor/xml_to_json_converter/src:/data \
		-e OP=test \
		python3.6_ci:latest

xml_to_json_converter-publish: xml_to_json_converter-build $(ROOT)/.docker/publish_service_to_aws
	$(ROOT)/builds/docker_run.py --aws -- \
	    -v /var/run/docker.sock:/var/run/docker.sock \
		-v $(ROOT):/repo \
		publish_service_to_aws:latest \
		--project=xml_to_json_converter \
		--infra-bucket=platform-infra