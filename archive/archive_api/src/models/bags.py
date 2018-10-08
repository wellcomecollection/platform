# -*- encoding: utf-8
"""
Implements the "Bag" model, as returned by the /bags/xx-xx-xx endpoint.
This is an example of response from that endpoint, based on the RFC at commit 8e2a483:
   {
     "@context": "https://api.wellcomecollection.org/storage/v1/context.json",
     "type": "Bag",
     "id": "xx-xx-xx-xx",
     "source": {
       "type": "Source",
       "id": "goobi",
       "label": "Goobi"
     },
     "identifiers": [
       {
         "type": "Identifier",
         "identifierType": {
           "id": "sierra-system-number",
           "label": "Sierra system number",
           "type": "IdentifierType"
         },
         "value": "b24923333"
       },
       {
         "type": "Identifier",
         "identifierType": {
           "id": "goobi-process-title",
           "label": "Goobi process title",
           "type": "IdentifierType"
         },
         "value": "12324_b_b24923333"
       },
       {
         "type": "Identifier",
         "identifierType": {
           "id": "goobi-process-id",
           "label": "Goobi process identifier",
           "type": "IdentifierType"
         },
         "value": "170131"
       }
     ],
     "manifest": {
       "type": "FileManifest",
       "checksumAlgorithm": "sha256",
       "files": [
         {
           "type": "File",
           "checksum": "a20eee...25d6f",
           "path": "data/b24923333.xml"
         },
         {
           "type": "File",
           "checksum": "e68c9...56a7e",
           "path": "data/objects/b24923333_001.jp2"
         },
         {
           "type": "File",
           "checksum": "17c01...aa502",
           "path": "data/alto/b24923333_001.xml"
         }
       ]
     },
     "tagManifest": {
       "type": "FileManifest",
       "checksumAlgorithm": "sha256",
       "files": [
         {
           "type": "File",
           "checksum": "791ea...cd44e",
           "path": "manifest-256.txt"
         },
         {
           "type": "File",
           "checksum": "13f83...d56e7",
           "path": "bag-info.txt"
         },
         {
           "type": "File",
           "checksum": "a39e0...76d37",
           "path": "bagit.txt"
         }
       ]
     },
     "archiveUrl" : "s3://archivebucket/digitised/b24923333/" ,
     "replicaUrl" : "https://example.org/digitised/b24923333/" ,
     "accessUrl" : "s3://accessbucket/digitised/b24923333/" ,
     "description": "A account of a voyage to New South Wales",
     "size": 435255.8,
     "createdDate": "2016-08-07T00:00:00Z",
     "lastModifiedDate": "2016-08-07T00:00:00Z",
     "version": 1
   }
"""

from flask_restplus import fields

from ._base import TypedModel
from .catalogue import Identifier


Source = TypedModel(
    "Source",
    {
        "id": fields.String(description="The identifier of the source"),
        "label": fields.String(description="The name of the source"),
    },
)


# This model matches "IdentifierType" in the Catalogue API.
IdentifierType = TypedModel(
    "IdentifierType", {"id": fields.String(), "label": fields.String()}
)


File = TypedModel("File", {"checksum": fields.String(), "path": fields.String()})


FileManifest = TypedModel(
    "File", {"checksumAlgorithm": fields.String(), "files": fields.List(fields.Nested(File))}
)


Bag = TypedModel(
    "Bag",
    {
        "id": fields.String(
            description="The canonical identifier given to a thing", required=True
        ),
        "source": fields.Nested(
            Source, description="The source of the bag", required=True
        ),
        "identifiers": fields.List(
            fields.Nested(Identifier),
            description=(
                "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
            ),
        ),
        "manifest": fields.List(fields.Nested(FileManifest)),
        "tagManifest": fields.List(fields.Nested(FileManifest)),
        "archiveUrl": fields.String(),
        "replicaUrl": fields.String(),
        "accessUrl": fields.String(),
        "description": fields.String(),
        "size": fields.Float(),
        "createdDate": fields.String(),
        "lastModifiedDate": fields.String(),
        "version": fields.Integer(),
    },
)
