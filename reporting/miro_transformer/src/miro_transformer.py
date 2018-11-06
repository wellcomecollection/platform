from reporting_pipeline.hybrid_record_pipeline import process_messages
from transform import transform


def main(event, _, s3_client=None, es_client=None, index=None, doc_type=None):
    process_messages(event, transform, s3_client, es_client, index, doc_type)
