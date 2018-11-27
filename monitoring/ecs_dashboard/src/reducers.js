// @flow

import {
  REQUEST_SERVICES, RECEIVE_SERVICES, FAILED_RETRIEVAL
} from './actions'
import type { AppState } from './Store'
import type { ECSStatus } from './models'
import type { Action } from './actions'

function errorReducer(error: ?Error, action: Action): ?Error {
  switch (action.type) {
    case FAILED_RETRIEVAL:
      return action.error;
    default:
      return null;
  }
}

function ecsStatusReducer(ecsStatus: ECSStatus, action: Action): ECSStatus {
  switch (action.type) {
    case RECEIVE_SERVICES:
      return action.payload;
    case REQUEST_SERVICES:
    default:
      return ecsStatus;
  }
}

function rootReducer(state: AppState = { ecsStatus: { clusterList: [], lastUpdated: null }, error: null }, action: Action): AppState {
  return {
    ecsStatus: ecsStatusReducer(state.ecsStatus, action),
    error: errorReducer(state.error, action)
  }
}

export default rootReducer;
