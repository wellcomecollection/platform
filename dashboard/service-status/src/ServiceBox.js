// @flow

import React, { Component } from 'react';

class ServiceBox extends Component {
  props: {
    serviceName: string,
    desiredCount: number,
    pendingCount: number,
    runningCount: number,
    status: string
  }

  render() {
    return (
      <div className="Service">
        <p className="Service-Name">
	        My Service: {this.props.serviceName}
	      </p>
        <ul>
          <li>Desired: {this.props.desiredCount}</li>
          <li>Pending: {this.props.pendingCount}</li>
          <li>Running: {this.props.runningCount}</li>
        </ul>
        <p>Status: <span className="Service-Status">{this.props.status}</span></p>
      </div>
    );
  }
}


export default ServiceBox;
