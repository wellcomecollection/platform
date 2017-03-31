# Adapter lifecycle

An *adapter* is an application that pulls records from an external data
source into a DynamoDB table.  These typically run on a fixed schedule,
e.g. once a day.  Because adapters pull in data and then sit idle, we don't
run them continuously.

This document has some brief notes on how adapters are scheduled.

## Background

Each adapter is a task running in EC2 Container Service.  We turn on/turn off
the application by changing the number of desired instances of the task.

The desired count is 0 (off) by default.

## Startup

An adapter is started by the following procedure:

1.  Some trigger (e.g. a CloudWatch event) posts a notification to the
    *service_scheduler* SNS topic.  This update identifies the cluster, service
    name, and has a desired count of 1.

    ```json
    {
      "cluster": "name-of-cluster",
      "service": "name-of-adapter",
      "desired_count": 1
    }
    ```

2.  We have a Lambda triggered by notifications to the SNS topic, which uses
    the AWS API to change the desired count of this service.

3.  ECS notices the change in desired count, and starts a new container.
    The adapter is now running.

## Shutdown

We can't simply call `System.exit(0)` to shut down an adapter, because ECS
would notice the application had terminated, and start another instance to
maintain uptime, aka the exact opposite of what we want.

So when an adapter wants to shut down, it does the following:

1.  The adapter sends another notification to the SNS topic:

    ```json
    {
      "cluster": "name-of-cluster",
      "service": "name-of-adapter",
      "desired_count": 0
    }
    ```

2.  As before, the Lambda notices this change, and updates the desired count
    in ECS.

3.  ECS tears down the container associated with the service, and the adapter
    is terminated.
