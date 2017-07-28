// @flow

import React, { Component } from 'react';
import type { Services } from './models'
import ServiceList from './ServiceList'
import './ClusterBox.css';

class ClusterBox extends Component {
  props: {
    clusterName: string,
    instanceCount: number,
    serviceList: Array<Services>
  }

  render() {
    return (
      <div className="ClusterBox">
        <div className="ClusterBox--Header">
          {this.props.clusterName} ({this.props.instanceCount})
        </div>
        <div className="ClusterBox--Body">

          <ServiceList
            key={this.props.clusterName}
            serviceList={this.props.serviceList}
          />

        </div>
      </div>
    );
  }
}

export default ClusterBox;
