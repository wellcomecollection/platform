// @flow

export type Service = {
  serviceName: string,
  desiredCount: number,
  pendingCount: number,
  runningCount: number,
  status: string
}

export type Services = Array<Service>
