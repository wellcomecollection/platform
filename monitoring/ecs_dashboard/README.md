# ECS Service Status Dashboard

A simple dashboard displaying AWS ECS service status.

## Setup

To run:

```sh
npm install
npm start
```

Requires a file `./src/config.js` of the format:

```js
// @flow

import type {Config} from './models'

const AppConfig: Config = {
  serviceListLocation: 'https://example.com/status/services.json'
}

export default AppConfig;
```

Serving a file of the format:

```js
[
  {
    "serviceName": "MyService",
    "desiredCount": 1,
    "pendingCount": 0,
    "runningCount": 1,
    "status": "ACTIVE"
  }
]
```

## Deployment

To deploy:

```sh
./deploy.sh my-bucket https://s3-eu-west-1.amazonaws.com/my-bucket
```
