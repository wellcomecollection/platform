// @flow

import { connect } from 'react-redux'
import LastUpdated from './LastUpdated'

function getProps(statePart) {
  return {time: statePart.lastUpdated};
}

const mapStateToProps = (state) => {
  return getProps(state.ecsStatus);
}

const VisibleLastUpdated = connect(
  mapStateToProps
)(LastUpdated)

export default VisibleLastUpdated
