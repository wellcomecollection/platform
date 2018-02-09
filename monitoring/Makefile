ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT  = monitoring

SBT_APPS    =
ECS_TASKS   = slack_budget_bot
LAMBDAS     = post_to_slack terraform_tracker

TF_NAME     = monitoring
TF_PATH     = $(ROOT)/$(STACK_ROOT)
TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))

include $(ROOT)/monitoring/deployment_tracking/Makefile
include $(ROOT)/monitoring/ecs_dashboard/Makefile
include $(ROOT)/monitoring/load_test/Makefile


monitoring-test: gatling_to_cloudwatch-test \
				 notify_old_deploys-test \
				 post_to_slack-test \
				 service_deployment_status-test \
				 update_service_list-test

monitoring-publish:	gatling_to_cloudwatch-publish \
					gatling-publish \
					notify_old_deploys-publish \
					post_to_slack-publish \
					service_deployment_status-publish \
					update_service_list-publish
