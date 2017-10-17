// @flow

import type { ECSStatus } from './models'

export type AppState = {
  ecsStatus: ECSStatus,
  error: ?Error
}
