// @flow

import type { Services } from './models'

export type AppState = {
  time: Date,
  services: Services,
  error: ?Error
}
