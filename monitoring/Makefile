ROOT = $(shell git rev-parse --show-toplevel)
MONITORING = $(ROOT)/monitoring

include $(MONITORING)/deployment_tracking/Makefile
include $(MONITORING)/ecs_dashboard/Makefile
include $(MONITORING)/load_test/Makefile

include $(ROOT)/functions.Makefile


post_to_slack-build:
	$(call build_lambda,monitoring/post_to_slack)

post_to_slack-test:
	$(call test_lambda,monitoring/post_to_slack)

post_to_slack-publish:
	$(call publish_lambda,monitoring/post_to_slack)


monitoring-test: post_to_slack-test deployment_tracking-test ecs_dashboard-test load_test-test

monitoring-publish: post_to_slack-publish deployment_tracking-publish ecs_dashboard-publish load_test-publish


monitoring-terraform-plan:
	$(call terraform_plan,$(MONITORING),false)

monitoring-terraform-apply:
	$(call terraform_apply,$(MONITORING))
