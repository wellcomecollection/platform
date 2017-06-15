// @flow

import { connect } from 'react-redux'
import LastUpdated from './LastUpdated'

function getProps(statePart) {
  return {time: statePart};
}

const mapStateToProps = (state) => {
  return getProps(state.time)
}

const VisibleLastUpdated = connect(
  mapStateToProps
)(LastUpdated)

export default VisibleLastUpdated
