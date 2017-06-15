// @flow

import type {Services} from './models'
import AppConfig from './config'

export type Action  = { type: string, payload: Services }

function requestServices(): Action {
  return { type: REQUEST_SERVICES, payload: []};
}

function receiveServices(json: Services): Action {
  const services = json

  return { type: RECEIVE_SERVICES, payload: services };
}

function fetchServices() {
  return (dispatch: Function) => {
    dispatch(requestServices());
    return fetch(AppConfig.serviceListLocation)
      .then(response => response.json())
      .then(json => dispatch(receiveServices(json)));
  }
}

export const REQUEST_SERVICES = 'REQUEST_SERVICES'
export const RECEIVE_SERVICES = 'RECEIVE_SERVICES'

export {
  fetchServices
}
