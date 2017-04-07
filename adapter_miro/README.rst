MIRO adapter
============

This directory contains some scripts for pulling data from the MIRO XML
exports into a DynamoDB table.

Initially the aim is just to import the image data, not the contributor
or editorial photography data.

Installation
------------

These scripts require Python 3 and dependencies:

.. code-block:: console

   $ brew install python3
   $ pip3 install boto3 lxml

Usage
-----

Invoke with the name of the DynamoDB table and the path to the export file:

.. code-block:: console

   $ python3 miro_adapter.py --table=MyMiroExportTable /path/to/export.xml
