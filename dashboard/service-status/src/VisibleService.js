// @flow

import { connect } from 'react-redux'
import Service from './Service'

function getProps(statePart) {
  return statePart;
}

const mapStateToProps = (state) => {
  return {
    name: getProps(state.placeholder)
  }
}

const VisibleService = connect(
  mapStateToProps
)(Service)

export default VisibleService
