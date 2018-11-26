# monitoring

This directory has the source and Terraform code for our monitoring infrastructure.

## ECS dashboard

We have a dashboard which reports the state of our ECS clusters.
For each cluster, it shows:

*   the number of registered EC2 instances
*   the registered services, and their desired/running/pending task counts
*   the running task definitions

<img src="docs/services_dashboard.png">

The live dashboard can be viewed at <https://s3-eu-west-1.amazonaws.com/wellcome-platform-dash/index.html>.

## Grafana dashboard

We have a [Grafana][grafana] dashboard for monitoring load tests, queue sizes, and our AWS bill, among other things.

It can be viewed at <https://monitoring.wellcomecollection.org/> (note this is only accessible from within the Wellcome IP range).

[grafana]: https://grafana.com/

## Slack alarms

We have an AWS Lambda that publishes certain CloudWatch alarms to a Slack channel, so failures are immediately visible.

<img src="docs/slack_alarm.png">
