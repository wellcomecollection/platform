// @flow

import {
  REQUEST_SERVICES, RECEIVE_SERVICES
} from './actions'
import type { AppState } from './Store'
import type { Services } from './models'

import type { Action } from './actions'

function placeholderReducer(placeholder: string, action: Action): string {
  switch (action.type) {
    case "UPDATE":
      return Date.now().toString()
    default:
      return placeholder;
  }
}

function servicesReducer(services: ?Services, action: Action): ?Services {
  switch (action.type) {
    case REQUEST_SERVICES:
      return services;
    case RECEIVE_SERVICES:
      return action.payload;
    default:
      return services;
  }
}

function rootReducer(state: AppState = { placeholder: "init", services: null }, action: Action): AppState {
  return {
    placeholder: placeholderReducer(state.placeholder, action),
    services: servicesReducer(state.services, action)
  }
}

export default rootReducer;
