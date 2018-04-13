# Because CloudFront lives in us-east-1, all the associated CloudWatch metrics
# (e.g. 4xx error rate) also live in us-east-1.  Since we want to monitor
# some of these metrics, this stack exists just to define that small pool
# of resources.

ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	=

SBT_APPS 	=
ECS_TASKS 	=
LAMBDAS 	=

TF_NAME 	= loris_cloudfront_monitoring
TF_PATH 	= loris/loris_cloudfront_monitoring
TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))
