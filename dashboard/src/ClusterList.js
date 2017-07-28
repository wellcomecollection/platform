// @flow

import React, { Component } from 'react';
import type { ECSStatus } from './models'
import ClusterBox from './ClusterBox'
import ErrorBox from './ErrorBox'
import './ClusterList.css'

class ClusterList extends Component {
  props: {
    ecsStatus: ECSStatus,
    error: ?Error
  }

  render() {
      if(this.props.error !== null) {
        return <ErrorBox error={this.props.error}/>
      } else {
        return <div className='ClusterList'>
          {this.props.ecsStatus.clusterList.map(cluster =>
            <ClusterBox key={cluster.clusterName}
            {...cluster}
            />
          )}
        </div>
    }
  }
}


export default ClusterList;
