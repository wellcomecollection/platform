# -*- encoding: utf-8 -*-

import operator


def identify_cluster_by_app_name(client, app_name):
    """
    Given the name of one of our applications (e.g. api, calm_adapter),
    return the ARN of the cluster the task runs on.
    """
    for cluster in client.list_clusters()['clusterArns']:
        for serviceArn in client.list_services(cluster=cluster)['serviceArns']:

            # The format of an ECS service ARN is:
            #
            #     arn:aws:ecs:{aws_region}:{account_id}:service/{service_name}
            #
            # Our ECS cluster is configured so that the name of the ECS cluster
            # matches the name of the config in S3.  It would be more robust
            # to use the describeService API, but this saves us a couple of
            # calls on our API quota so we skip it.
            _, serviceName = serviceArn.split('/')
            if serviceName == app_name:
                return cluster

    raise RuntimeError(f'Unable to find ECS cluster for {app_name}')


def get_latest_task_definition(client, cluster, service):
    """
    Given the name of a cluster and a service, return the ARN
    for its latest task definition.
    """
    resp = client.describe_services(cluster=cluster, services=[service])

    # The top-level structure of a describeServices API response is of the form
    #
    #     {
    #       "failures": [],
    #       "services": [
    #         ...
    #       ]
    #     }
    #
    # Because we only asked for a description of a single service, we expect
    # there to only be a single service.
    services = resp['services']
    assert len(services) == 1, resp
    service = services[0]

    # Within a 'service' description, the following structure is what we're
    # interested in:
    #
    #     "deployments": [
    #       {
    #         "createdAt": <date>,
    #         "taskDefinition": <task definition ARN>,
    #         "updatedAt": <date>
    #         ...
    #       },
    #       ... other running tasks
    #     ],
    #
    # Each "deployment" corresponds to a running task, so we pick the
    # container with the most recent creation date.
    deployments = service['deployments']
    assert len(deployments) > 0, resp
    newest_deployment = max(deployments, key=operator.itemgetter('createdAt'))

    return newest_deployment['taskDefinition']


def clone_task_definition(client, task_definition):
    """
    Given a task definition ARN, clone the associated task.

    Returns the new task definition ARN.
    """
    resp = client.describe_task_definition(taskDefinition=task_definition)
    taskDefinition = resp['taskDefinition']

    # The task definition contains two key fields: "family" and
    # "containerDefinitions" which full describe the task.
    new_task = client.register_task_definition(
        family=taskDefinition['family'],
        containerDefinitions=taskDefinition['containerDefinitions']
    )

    return new_task['taskDefinition']['taskDefinitionArn']
