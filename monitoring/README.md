# Monitoring

## Grafana dashboard

We have a [Grafana][grafana] dashboard for monitoring load tests, queue sizes, and our AWS bill, among other things.

It can be viewed at <https://monitoring.wellcomecollection.org/> (note this is only accessible from within the Wellcome IP range).

[grafana]: https://grafana.com/

## Slack alarms

We have an AWS Lambda that publishes certain CloudWatch alarms to a Slack channel, so failures are immediately visible.