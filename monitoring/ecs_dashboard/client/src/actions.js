// @flow

import type {ECSStatus} from './models'
import AppConfig from './config'

export type Action  = { type: string, payload: ECSStatus, error: ?Error}

function requestStatus(): Action {
  return { type: REQUEST_SERVICES, payload: { clusterList: [] }, error: null };
}

function receiveStatus(ecsStatus: ECSStatus): Action {
  return { type: RECEIVE_SERVICES, payload: ecsStatus, error: null };
}

function failedToGetStatus(reason: Error): Action {
  return { type: FAILED_RETRIEVAL, payload: { clusterList: [] }, error: reason };
}

function fetchServices() {
  return (dispatch: Function) => {
    dispatch(requestStatus());
    return fetch(AppConfig.serviceListLocation)
      .then(response => response.json())
      .then(json => dispatch(receiveStatus(json)))
      .catch(reason => dispatch(failedToGetStatus(reason)));
  }
}

export const FAILED_RETRIEVAL = 'FAILED_RETRIEVAL'
export const REQUEST_SERVICES = 'REQUEST_SERVICES'
export const RECEIVE_SERVICES = 'RECEIVE_SERVICES'

export {
  fetchServices
}
