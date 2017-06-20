// @flow

export type ECSStatus = {
  clusterList: Array<Cluster>,
  lastUpdated: ?Date
}

export type Cluster = {
  clusterName: string,
  instanceCount: number,
  serviceList: Array<Service>,
  status: string
}

export type Deployment = {
  id: string,
  taskDefinition: string,
  status: string
}

export type Event = {
  timestamp: number,
  message: string
}

export type Service = {
  serviceName: string,
  desiredCount: number,
  pendingCount: number,
  runningCount: number,
  deployments: Array<Deployment>,
  events: Array<Event>,
  status: string
}

export type Config = {
  serviceListLocation: string
}
