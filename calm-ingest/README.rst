calm-ingest
===========

This directory contains some rudimentary Python scripts to help ingest
Calm data.

Ingest process
**************

This diagram displays the current ingest architecture:

.. image:: ingest_architecture.png

We have a number of data sources (initially just Calm, but we'll add others).
An adapter ingests all the records into a per-source DynamoDB table, treating
Dynamo as a mirror of the original source.

A transformer runs on the other side of each Dynamo table, and parses out the
fields we want to expose on Elasticsearch.  These parsed records are sent to
per-source SNS topics, which are in turn coalesced into a single SQS queue.
A worker pulls entries from the queue into the Elasticsearch index, which is
then queried by our API.

Installation
************

These scripts require Python 3.  To install dependencies:

.. code-block:: console

   $ python3 -m venv env
   $ source env/bin/activate
   $ pip install -r requirements.txt

Usage
*****

Once you have the requirements installed, the ingest is a two-step process:

1. Given an XML export file from Calm, push the records into DynamoDB:

   .. code-block:: console

      $ python3 calm_to_dynamodb.py /path/to/calm_export.xml
