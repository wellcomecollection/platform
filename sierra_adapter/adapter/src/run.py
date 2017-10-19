#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Python script for doing basic requests against Sierra.

To configure:

*   Fill in the API key and API secret you got from Sierra
*   Change the URL to point to the resource you want

"""

import json
from pprint import pprint
import sys

import requests


oauthkey = '<API_KEY>'
oauthsec = '<API_SECRET>'

BASE = 'https://libsys.wellcomelibrary.org/iii'
API_VERSION = 'sierra-api/v3'

# Change this URL to whatever resource you want to retrieve
URL = f'{BASE}/{API_VERSION}/branches'


# Get an access token
# https://sandbox.iii.com/docs/Content/zReference/authClient.htm
resp = requests.post(f'{BASE}/{API_VERSION}/token', auth=(oauthkey, oauthsec))
resp.raise_for_status()
access_token = resp.json()['access_token']

# https://sandbox.iii.com/docs/Content/zReference/authClient.htm
# https://sandbox.iii.com/docs/Content/zReference/operations.htm#XML
headers = {
    'Authorization': f'Bearer {access_token}'
    'Accept': 'application/json',
}

print('*** Fetching contents of %s' % URL)
resp = requests.get(URL, headers=headers)
resp.raise_for_status()

pprint(resp.json())
