// @flow

import {
  REQUEST_SERVICES, RECEIVE_SERVICES, FAILED_NETWORK
} from './actions'
import type { AppState } from './Store'
import type { Services } from './models'
import type { Action } from './actions'

function errorReducer(error: ?Error, action: Action): ?Error {
  switch (action.type) {
    case FAILED_NETWORK:
      return action.error;
    default:
      return null;
  }
}

function servicesReducer(services: Services, action: Action): Services {
  switch (action.type) {
    case RECEIVE_SERVICES:
      return action.payload;
    case REQUEST_SERVICES:
    default:
      return services;
  }
}

function rootReducer(state: AppState = { services: { services: [] }, error: null }, action: Action): AppState {
  return {
    services: servicesReducer(state.services, action),
    error: errorReducer(state.error, action)
  }
}

export default rootReducer;
