// @flow

import {
  REQUEST_SERVICES, RECEIVE_SERVICES, FAILED_NETWORK
} from './actions'
import type { AppState } from './Store'
import type { Services } from './models'

import type { Action } from './actions'

function timeReducer(time: Date, action: Action): Date {
  switch (action.type) {
    case "UPDATE":
      return new Date();
    default:
      return time;
  }
}

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
    case REQUEST_SERVICES:
      return services;
    case RECEIVE_SERVICES:
      return action.payload;
    default:
      return services;
  }
}

function rootReducer(state: AppState = { time: "init", services: [], error: null}, action: Action): AppState {
  return {
    time: timeReducer(state.time, action),
    services: servicesReducer(state.services, action),
    error: errorReducer(state.error, action)
  }
}

export default rootReducer;
