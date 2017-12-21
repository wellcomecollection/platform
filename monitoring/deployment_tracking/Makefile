ROOT = $(shell git rev-parse --show-toplevel)

include $(ROOT)/functions.Makefile


notify_old_deploys-test:
	$(call test_lambda,monitoring/deployment_tracking/notify_old_deploys)

notify_old_deploys-publish:
	$(call publish_lambda,monitoring/deployment_tracking/notify_old_deploys)


service_deployment_status-test:
	$(call test_lambda,monitoring/deployment_tracking/service_deployment_status)

service_deployment_status-publish:
	$(call publish_lambda,monitoring/deployment_tracking/service_deployment_status)


deployment_tracking-test: notify_old_deploys-test service_deployment_status-test

deployment_tracking-publish: notify_old_deploys-publish service_deployment_status-publish
