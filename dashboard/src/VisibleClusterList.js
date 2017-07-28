// @flow

import { connect } from 'react-redux'
import ClusterList from './ClusterList'

function getProps(ecsStatus, error) {
  return {
    ecsStatus: ecsStatus,
    error: error
  };
}

const mapStateToProps = (state) => {
  return getProps(state.ecsStatus, state.error)
}

const VisibleClusterList = connect(
  mapStateToProps
)(ClusterList)

export default VisibleClusterList
