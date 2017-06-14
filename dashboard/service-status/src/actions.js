// @flow

import type {Services} from './models'

export type Action  = { type: string, payload: ?Services }

function requestServices(): Action {
  return { type: REQUEST_SERVICES, payload: null};
}

function receiveServices(json: Services): Action {
  console.log(json);

  const services = json

  return { type: RECEIVE_SERVICES, payload: services };
}

function fetchServices() {
  return (dispatch: Function) => {
    dispatch(requestServices());
    return fetch(`https://s3-eu-west-1.amazonaws.com/muh-bukkit/status/services.json`)
      .then(response => response.json())
      .then(json => dispatch(receiveServices(json)));
  }
}

export const REQUEST_SERVICES = 'REQUEST_SERVICES'
export const RECEIVE_SERVICES = 'RECEIVE_SERVICES'

export {
  fetchServices
}
