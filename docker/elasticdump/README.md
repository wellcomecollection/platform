# elasticdump

This task can be triggered to get a complete dump of records from an Elasticsearch index, which are uploaded to an S3 bucket.

## Usage

This Docker image is kept as a task definition in ECS, and can be invoked as a one-off task using the RunTask API.

You need to pass the name of the index that you want to dump as an environment variable `INDEX`.

For example:

```
aws ecs run-task --cluster=services_cluster \
    --task-definition=elasticdump_task_definition \
    --overrides='{
      "containerOverrides": [
        {
          "name": "app",
          "environment": [
            { "name": "INDEX", "value": "works-with-thumbnails" }
          ]
        }
      ]
    }'
```

Assuming the task completes successfully, the dump is written to the `elasticdump` folder in our `platform-infra` bucket.

## Deployment

This task is built and deployed automatically by Travis.
