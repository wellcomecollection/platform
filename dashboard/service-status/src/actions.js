// @flow

import type {Services} from './models'
import AppConfig from './config'

export type Action  = { type: string, payload: Services, error: ?Error}

function requestServices(): Action {
  return { type: REQUEST_SERVICES, payload: [], error: null };
}

function receiveServices(json: Services): Action {
  const services = json

  return { type: RECEIVE_SERVICES, payload: services, error: null };
}

function failedNetwork(reason: Error): Action {
  return { type: FAILED_NETWORK, payload: [], error: reason };
}

function fetchServices() {
  return (dispatch: Function) => {
    dispatch(requestServices());
    return fetch(AppConfig.serviceListLocation)
      .then(response => response.json())
      .then(json => dispatch(receiveServices(json)))
      .catch(reason => dispatch(failedNetwork(reason)));
  }
}

export const FAILED_NETWORK   = 'FAILED_NETWORK'
export const REQUEST_SERVICES = 'REQUEST_SERVICES'
export const RECEIVE_SERVICES = 'RECEIVE_SERVICES'

export {
  fetchServices
}
