# -*- coding: utf-8 -*-


class SQSReader:
    _current_message = None

    def __init__(self, sqs_client, queue_url, max_messages=5):
        self.sqs_client = sqs_client
        self.queue_url = queue_url
        self.max_message = max_messages

    def __iter__(self):
        while True:
            response = self.sqs_client.receive_message(
                QueueUrl=self.queue_url,
                MaxNumberOfMessages=self.max_message
            )

            if "Messages" in response:
                messages = response["Messages"]
                for message in messages:
                    self._current_message = message
                    yield message

    def delete_current(self):
        if self._current_message is not None:
            self.sqs_client.delete_message(
                QueueUrl=self.queue_url,
                ReceiptHandle=self._current_message['ReceiptHandle']
            )
