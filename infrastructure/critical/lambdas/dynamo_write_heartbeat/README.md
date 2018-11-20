#dynamo_write_heartbeat

Run a no-op write on DynamoDB (delete a non existent item) to be run in a scheduled lambda to make DynamoDB scale down when there would otherwise be zero throughput and it otherwise wouldn't.

TABLE_NAMES: comma separated list of dynamoDb tables to call