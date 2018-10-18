import boto3


def filter_queue_attributes(attributes):
    visible = int(attributes['ApproximateNumberOfMessages'])
    delayed = int(attributes['ApproximateNumberOfMessagesDelayed'])
    in_flight = int(attributes['ApproximateNumberOfMessagesNotVisible'])

    visible_and_in_flight = visible + in_flight

    return {
        'visible': visible,
        'delayed': delayed,
        'in_flight': in_flight,
        'visible_and_in_flight': visible_and_in_flight
    }


def put_metric(cloudwatch_client, namespace, metrics):
    return cloudwatch_client.put_metric_data(
        MetricData=metrics,
        Namespace=namespace
    )


def get_queue_name(queue_url):
    return queue_url.split("/")[-1]


def build_metric(queue):
    name = get_queue_name(queue[0])
    value = queue[1]['visible_and_in_flight']

    return {
        'MetricName': name,
        'Dimensions': [],
        'Unit': 'None',
        'Value': value
    }


def update_queue_metrics(cloudwatch_client, sqs_client, sqs_resource):
    # Get queues
    queue_list_response = sqs_client.list_queues()

    # Extract QueueUrls
    all_queue_urls = queue_list_response['QueueUrls']

    # Filter out DLQs
    queues = [sqs_resource.Queue(queue_url) for queue_url in all_queue_urls if not queue_url.endswith('_dlq')]

    # Filter attributes and create visible_and_in_flight
    filtered_queues_attributes = [
        (q.url, filter_queue_attributes(q.attributes)) for q in queues]

    # Build metrics to send
    metrics = [build_metric(queue) for queue in filtered_queues_attributes]

    # Send metrics
    namespace = "queues/visible_and_in_flight"
    [put_metric(cloudwatch_client, namespace, [metric]) for metric in metrics]


def main(o, ctx):
    sqs_resource = boto3.resource('sqs')
    sqs_client = boto3.client('sqs')
    cloudwatch_client = boto3.client('cloudwatch')

    update_queue_metrics(cloudwatch_client, sqs_client, sqs_resource)
