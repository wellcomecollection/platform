// @flow

import type { Services } from './models'

export type AppState = {
  services: Services,
  error: ?Error
}
