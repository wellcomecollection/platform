# -*- encoding: utf-8 -*-


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
