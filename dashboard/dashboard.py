#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
A tiny service dashboard for our ECS deployment.
"""

import collections

import boto3
from tabulate import tabulate

Cluster = collections.namedtuple('Cluster', ['name', 'arn'])


# http://stackoverflow.com/a/312464/1558022
def _chunks(l, n):
    """Yield successive n-sized chunks from l."""
    for i in range(0, len(l), n):
        yield l[i:i + n]


def _paginated_api(api_method, *args, **kwargs):
    """Wraps a paginated call to the AWS API."""
    nextToken = ''
    while True:
        kwargs['nextToken'] = nextToken
        response = api_method(*args, **kwargs)
        yield response

        # If there are more results to return, the API includes a
        # "nextToken" parameter that we can use to fetch the next page.
        # If it is absent or null, thne we are on the final page.
        try:
            nextToken = response['nextToken']
        except KeyError:
            break

        if not nextToken:
            break


def get_clusters(client):
    """Generates a (name, ARN) tuple for every cluster."""
    for response in _paginated_api(client.list_clusters):
        description = client.describe_clusters(clusters=response['clusterArns'])
        for c in description['clusters']:
            yield Cluster(name=c['clusterName'], arn=c['clusterArn'])


def get_tasks(client, cluster):
    """Generates a task ARN for every task in a cluster."""
    for response in _paginated_api(client.list_tasks, cluster=cluster.name):
        for arn in response['taskArns']:
            yield arn


RunningTask = collections.namedtuple('RunningTask', ['arn', 'service', 'definition', 'date_started'])


def get_running_tasks(client, cluster, tasks):
    """Generates RunningTask's for every task in `tasks`."""
    tasks = list(tasks)

    # We can only fetch up to 100 task definitions at once
    for task_set in _chunks(tasks, 100):
        for t in client.describe_tasks(cluster=cluster.name, tasks=task_set)['tasks']:
            if t['lastStatus'] != 'RUNNING':
                continue
            yield RunningTask(
                arn=t['taskArn'],
                service=t['group'].replace('service:', ''),
                definition=t['taskDefinitionArn'],
                date_started=t['startedAt']
            )


def get_release_tag(client, task_definition_arn):
    """Returns the release ID tag for a task definition."""
    response = client.describe_task_definition(taskDefinition=task_definition_arn)
    containers = [
        c
        for c in response['taskDefinition']['containerDefinitions']
        if c['name'] == 'app'
    ]
    assert len(containers) == 1
    app_container = containers[0]
    return app_container['image'].split(':')[1]


if __name__ == '__main__':
    client = boto3.client('ecs')

    rows = []

    for cluster in get_clusters(client):
        tasks = get_tasks(client, cluster)
        running_tasks = get_running_tasks(client, cluster=cluster, tasks=tasks)
        for t in running_tasks:
            tag = get_release_tag(client, task_definition_arn=t.definition)
            rows.append([t.service, tag, t.date_started.strftime('%Y-%m-%d %H:%M:%S')])

    print(tabulate(rows, headers=['service', 'release ID', 'date started']))
