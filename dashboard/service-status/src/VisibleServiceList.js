// @flow

import { connect } from 'react-redux'
import ServiceList from './ServiceList'

function getProps(services, error) {
  return {
    services: services,
    isError: !(error === null),
    error: error
  };
}

const mapStateToProps = (state) => {
  return getProps(state.services, state.error)
}

const VisibleServiceList = connect(
  mapStateToProps
)(ServiceList)

export default VisibleServiceList
