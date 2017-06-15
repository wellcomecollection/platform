// @flow

import {
  REQUEST_SERVICES, RECEIVE_SERVICES
} from './actions'
import type { AppState } from './Store'
import type { Services } from './models'

import type { Action } from './actions'

function timeReducer(time: string, action: Action): string {
  switch (action.type) {
    case "UPDATE":
      return Date.now();
    default:
      return time;
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

function rootReducer(state: AppState = { time: "init", services: [] }, action: Action): AppState {
  return {
    time: timeReducer(state.time, action),
    services: servicesReducer(state.services, action)
  }
}

export default rootReducer;
