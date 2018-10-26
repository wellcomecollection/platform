import os
import transformer_example

def test_transformer_example():
    assert True is True

def create_sns_message():
    return { 'Records': [ {
        'EventSource': 'aws:sns',
        'EventSubscriptionArn': 'arn:aws:sns:eu-west-1:1111111111111111:reporting_miro_reindex_topic:7f684445-0aec-4dee-b3fc-aafc0e9afd67',
        'EventVersion': '1.0',
        'Sns': { 'Message': '{"id":"V0010033","version":2,"location":{"namespace":"wellcomecollection-vhs-sourcedata-miro","key":"33/V0010033/0.json"}}',
                 'MessageAttributes': {},
                 'MessageId': '108920ea-19a2-550f-8c2e-74a7163ad107',
                 'Signature': 'fxkrB6/WGHY8HkFqfsD4Ir6PtvIw97GZSXW7evIYposeyp/X+/TyOXOTkHo4WltsbJGL/udnRHP9gnBdZ7OWvynJMM76KPnb9/d8pRfb6EH6nD0kaTURQ1GIK7/UHiArtTqP8CHPCr+jxhcWHrX3WsxZV3jq4mkXm2PROFfvsVM3uGiinIFoXFJmBfTUvxygpOIaeg69nG+7CVkTvcggW+Tpm89KNZ+oWbkwrdMggDio23HbvgvW4caGSQ2Pha64LnW+hX1V4Opa+Sw8of47qPZJ1/YLK+NVNvyYO9ykSvbrrO1+wsj3aN60uE4DIqLPnP5fkrTydzBiw==',
                 'SignatureVersion': '1',
                 'SigningCertUrl': 'https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-ac565b8b1a6c5d992d285f9598aa1d9b.pem',
                 'Subject': 'HybridRecordSender',
                 'Timestamp': '2018-10-26T12:49:28.829Z',
                 'TopicArn': 'arn:aws:sns:eu-west-1:1111111111111111:reporting_miro_reindex_topic',
                 'Type': 'Notification',
                 'UnsubscribeUrl': 'https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe'
        }}]}

# TODO: Fix lambda to use test elastic search client
# def test(sns_client, s3_client, elasticsearch_url, elasticsearch_index):
#     event = create_sns_message()
#
#     os.environ["ES_URL"] = elasticsearch_url
#     os.environ["ES_USER"] = "elastic"
#     os.environ["ES_PASS"] = "changeme"
#     os.environ["ES_INDEX"] = elasticsearch_index
#     os.environ["ES_DOC_TYPE"] = "example"
#
#     transformer_example.main(event, {})

