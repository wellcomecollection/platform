#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
A tiny service dashboard for our ECS deployment.
"""

import collections

import boto3
from tabulate import tabulate


def _chunks(iterable, chunk_size):
    """Yield successive ``chunk_size``-sized chunks from ``iterable``."""
    # Based on http://stackoverflow.com/a/312464/1558022
    sequence = list(iterable)
    for i in range(0, len(sequence), chunk_size):
        yield sequence[i:i + chunk_size]


def _paginated_api(api_method, *args, **kwargs):
    """
    Wraps a paginated call to the AWS API.

    Some calls to the AWS API are paginated.  To get the next page of
    results, you use the `nextToken` field present in the current response.
    This wrapper takes an API method and its calling parameters, and
    generates the paginated responses.
    """
    nextToken = ''
    while True:
        kwargs['nextToken'] = nextToken
        response = api_method(*args, **kwargs)
        yield response

        # If the token is absent or null, then we are on the final page.
        try:
            nextToken = response['nextToken']
        except KeyError:
            break

        if not nextToken:
            break


def get_clusters(client):
    """Generates the name of every ECS cluster in a deployment."""
    for response in _paginated_api(client.list_clusters):
        # The structure of a cluster ARN is
        #
        #     arn:aws:ecs:{region}:{account_id}:cluster/{cluster_name}
        #
        # We obtain the cluster name by parsing the ARN rather than using
        # the DescribeClusters API to avoid wasting API calls.
        for c in response['clusterArns']:
            yield c.split('/')[1]


def get_task_arns(client, cluster):
    """Generates a task ARN for every task in a cluster."""
    for response in _paginated_api(client.list_tasks, cluster=cluster):
        yield from response['taskArns']


RunningTask = collections.namedtuple(
    'RunningTask',
    ['arn', 'service', 'definition', 'date_started'])


def get_running_tasks(client, cluster, tasks):
    """Generates RunningTask's for every task in `tasks`."""
    # The AWS APIs only let you get the description for 100 tasks at once.
    for task_set in _chunks(tasks, chunk_size=100):
        for t in client.describe_tasks(cluster=cluster, tasks=task_set)['tasks']:
            if t['lastStatus'] != 'RUNNING':
                continue

            # In our deployment, the values in the `group` field have been
            # of the form `service:api` or `service:id_minter`.
            #
            # I don't know exactly what the contents of this field are for;
            # this may not work in a general setting.
            yield RunningTask(
                arn=t['taskArn'],
                service=t['group'].replace('service:', ''),
                definition=t['taskDefinitionArn'],
                date_started=t['startedAt']
            )


def get_release_tag(client, taskDefinitionArn):
    """Returns the release ID tag for a task definition."""
    response = client.describe_task_definition(
        taskDefinition=taskDefinitionArn
    )

    # Our tasks typically have two containers: an 'app' container that runs
    # our Scala applications, and an 'nginx' container that runs our web
    # server.  We're only interested in the 'app' container.
    containers = [
        c
        for c in response['taskDefinition']['containerDefinitions']
        if c['name'] == 'app'
    ]
    assert len(containers) == 1
    app_container = containers[0]

    # A container image ID is of the form
    #
    #     {ECR_repo_URL}/uk.ac.wellcome/{service}:{version}-{git_hash}_prod
    #
    # The release ID is the part after the final colon.
    return app_container['image'].split(':')[1]


if __name__ == '__main__':
    client = boto3.client('ecs')

    rows = []

    for cluster in get_clusters(client):
        tasks = get_task_arns(client, cluster)
        running_tasks = get_running_tasks(client, cluster=cluster, tasks=tasks)
        for t in running_tasks:
            tag = get_release_tag(client, taskDefinitionArn=t.definition)
            rows.append([t.service, tag, t.date_started.strftime('%Y-%m-%d %H:%M:%S')])

    print(tabulate(rows, headers=['service', 'release ID', 'date started']))
