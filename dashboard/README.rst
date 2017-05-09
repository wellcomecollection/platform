dashboard
=========

This is a mini service dashboard for the state of our deployment.

When we do a deployment, it takes a few minutes for the updated containers to fan out through the system.
The dashboard tells us about all our running containers, when they were started, and which version they're running.
Here's an example of the output:

.. code-block:: console

   $ python dashboard.py
   service      release ID                                           date started
   -----------  ---------------------------------------------------  -------------------
   api          0.0.1-b55ca1cefeba77576e2cc8ef7bff55d107bdb8ab_prod  2017-05-08 14:36:13
   api          0.0.1-c21a83cbdd9f8b7acb96cac9facea8294fd95055_prod  2017-05-08 15:32:59
   transformer  0.0.1-c21a83cbdd9f8b7acb96cac9facea8294fd95055_prod  2017-05-08 15:32:45
   transformer  0.0.1-b55ca1cefeba77576e2cc8ef7bff55d107bdb8ab_prod  2017-05-08 15:26:42
   ingestor     0.0.1-b55ca1cefeba77576e2cc8ef7bff55d107bdb8ab_prod  2017-05-08 14:43:08
   id_minter    0.0.1-b55ca1cefeba77576e2cc8ef7bff55d107bdb8ab_prod  2017-05-08 14:50:40

Installation
************

This script requires Python 3 (``brew install python3``) and some dependencies (``pip install -r requirements.txt``).

Alternatively, this directory includes a Dockerfile:

.. code-block:: console

   $ cd platform-infra/dashboard
   $ docker build -t wellcome/dashboard .
   $ docker run -v ~/.aws:/root/.aws wellcome/dashboard

(If you get an ``InvalidSignatureException`` from boto3, the clock inside your Docker container has drifted.
You should restart Docker.)

Usage
*****

Make sure you have your AWS credentials configured (e.g. as environment variables, or in ``~/.aws/credentials``).
Then run the script with no arguments:

.. code-block:: console

   $ python dashboard.py

Possible enhancements
*********************

*  Currently it only shows running containers.
   Would pending/stopped containers be useful?

*  Highlight if a container is running an old release.

*  Prettier interface.
