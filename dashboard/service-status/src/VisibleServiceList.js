// @flow

import { connect } from 'react-redux'
import ServiceList from './ServiceList'

function getProps(statePart) {
  return {services: statePart};
}

const mapStateToProps = (state) => {
  return getProps(state.services)
}

const VisibleServiceList = connect(
  mapStateToProps
)(ServiceList)

export default VisibleServiceList
