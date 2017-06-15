// @flow

import React, { Component } from 'react';
import TaskCounter from './TaskCounter'
import './ServiceBox.css'

class ServiceBox extends Component {
  props: {
    serviceName: string,
    desiredCount: number,
    pendingCount: number,
    runningCount: number,
    status: string
  }

  render() {
    const isStable = (this.props.desiredCount === this.props.runningCount) && this.props.pendingCount === 0

    return (
      <div className={'ServiceBox ' + (isStable ? 'ServiceBox__Stable' : 'ServiceBox__NotStable')}>
        <div className="ServiceBox--Header">
          {this.props.serviceName}
	      </div>
        <div className="ServiceBox--Body">
          <div className="TaskCounterHolder">
            <TaskCounter type="Desired" count={this.props.desiredCount} shouldBeZero={false}/>
            <TaskCounter type="Running" count={this.props.runningCount} shouldBeZero={false}/>
            <TaskCounter type="Pending" count={this.props.pendingCount} shouldBeZero={true}/>
          </div>
          <div className="ServiceBox--Status ">{this.props.status}</div>
        </div>
      </div>
    );
  }
}


export default ServiceBox;
