import datetime

import boto3

import task_tracking


def create_task(**kwargs):
    task =  {
        'clusterArn': 'arn:aws:ecs:<region>:<aws_account_id>:cluster/default',
        'containerInstanceArn': 'arn:aws:ecs:<region>:<aws_account_id>:container-instance/18f9eda5-27d7-4c19-b133-45adc516e8fb',
        'containers': [
            {
                'name': 'ecs-demo',
                'containerArn': 'arn:aws:ecs:<region>:<aws_account_id>:container/7c01765b-c588-45b3-8290-4ba38bd6c5a6',
                'lastStatus': 'RUNNING',
                'networkBindings': [
                    {
                        'bindIP': '0.0.0.0',
                        'containerPort': 80,
                        'hostPort': 80,
                    },
                ],
                'taskArn': 'arn:aws:ecs:<region>:<aws_account_id>:task/c5cba4eb-5dad-405e-96db-71ef8eefe6a8',
            },
        ],
        'desiredStatus': 'RUNNING',
        'lastStatus': 'RUNNING',
        'overrides': {
            'containerOverrides': [
                {
                    'name': 'ecs-demo',
                },
            ],
        },
        'startedBy': 'ecs-svc/9223370608528463088',
        'taskArn': 'arn:aws:ecs:<region>:<aws_account_id>:task/c5cba4eb-5dad-405e-96db-71ef8eefe6a8',
        'taskDefinitionArn': 'arn:aws:ecs:<region>:<aws_account_id>:task-definition/amazon-ecs-sample:1',
    }

    task.update(kwargs)

    return task


def test_get_tasks_from_ecs(ecs_task):
    task, task_definition, cluster_name, cluster_arn, container_instance_arn = ecs_task

    ecs_client = boto3.client('ecs')

    actual_tasks = task_tracking.get_tasks_from_ecs(
        ecs_client,
        cluster_name
    )

    expected_tasks = [
        task_tracking.Task(
            task_tracking.TaskKey(
                task_arn=task['taskArn'],
                task_definition_arn=task_definition['taskDefinitionArn']
            ),
            started_at=None,
            completed=False,
            success=False
        )
    ]

    assert actual_tasks == expected_tasks


def test_create_task_tuple_from_task_started_at():
    started_at_datetime = datetime.datetime(2015, 1, 1)
    task = create_task(startedAt=started_at_datetime)

    assert task_tracking.create_task_tuple_from_task(task) == (
        task_tracking.Task(
            task_key=task_tracking.TaskKey(
                task_arn='arn:aws:ecs:<region>:<aws_account_id>:task/c5cba4eb-5dad-405e-96db-71ef8eefe6a8',
                task_definition_arn='arn:aws:ecs:<region>:<aws_account_id>:task-definition/amazon-ecs-sample:1'
            ),
            started_at='2015-01-01T00:00:00Z',
            completed=False,
            success=False
        )
    )


def test_create_task_tuple_from_task_started_at_empty():
    task = create_task()

    assert task_tracking.create_task_tuple_from_task(task) == (
        task_tracking.Task(
            task_key=task_tracking.TaskKey(
                task_arn='arn:aws:ecs:<region>:<aws_account_id>:task/c5cba4eb-5dad-405e-96db-71ef8eefe6a8',
                task_definition_arn='arn:aws:ecs:<region>:<aws_account_id>:task-definition/amazon-ecs-sample:1'
            ),
            started_at=None,
            completed=False,
            success=False
        )
    )


def test_create_task_tuple_from_task_completed_true():
    task = create_task(lastStatus='STOPPED')

    assert task_tracking.create_task_tuple_from_task(task) == (
        task_tracking.Task(
            task_key=task_tracking.TaskKey(
                task_arn='arn:aws:ecs:<region>:<aws_account_id>:task/c5cba4eb-5dad-405e-96db-71ef8eefe6a8',
                task_definition_arn='arn:aws:ecs:<region>:<aws_account_id>:task-definition/amazon-ecs-sample:1'
            ),
            started_at=None,
            completed=True,
            success=False
        )
    )


def test_create_task_tuple_from_task_completed_false():
    task = create_task(lastStatus='RUNNING')

    assert task_tracking.create_task_tuple_from_task(task) == (
        task_tracking.Task(
            task_key=task_tracking.TaskKey(
                task_arn='arn:aws:ecs:<region>:<aws_account_id>:task/c5cba4eb-5dad-405e-96db-71ef8eefe6a8',
                task_definition_arn='arn:aws:ecs:<region>:<aws_account_id>:task-definition/amazon-ecs-sample:1'
            ),
            started_at=None,
            completed=False,
            success=False
        )
    )


def test_create_task_tuple_from_task_success_true():
    task = create_task()

    assert task_tracking.create_task_tuple_from_task(task) == (
        task_tracking.Task(
            task_key=task_tracking.TaskKey(
                task_arn='arn:aws:ecs:<region>:<aws_account_id>:task/c5cba4eb-5dad-405e-96db-71ef8eefe6a8',
                task_definition_arn='arn:aws:ecs:<region>:<aws_account_id>:task-definition/amazon-ecs-sample:1'
            ),
            started_at=None,
            completed=False,
            success=True
        )
    )
