// @flow

import React, { Component } from 'react';
import TaskCounter from './TaskCounter';
import type { Event, Deployment } from './models';
import EventBox from './EventBox';
import DeploymentList from './DeploymentList'
import './ServiceBox.css';

class ServiceBox extends Component {
  props: {
    serviceName: string,
    desiredCount: number,
    pendingCount: number,
    runningCount: number,
    events: Array<Event>,
    deployments: Array<Deployment>,
    status: string
  }

  render() {
    const isStable = (this.props.desiredCount === this.props.runningCount) && this.props.pendingCount === 0
    const isUnderProvisioned = this.props.desiredCount > this.props.runningCount

    const classModifier = (() => {
      if(isUnderProvisioned) {
        return "Bad";
      } else {
        if(isStable) {
          return "Stable";
        } else {
          return "NotStable";
        }
      }
    })();

    return (
      <div className={'ServiceBox ServiceBox__' + classModifier }>
        <div className="ServiceBox--Header">
          <a className="ServiceBox--Header__Name">{this.props.serviceName}</a>
          <EventBox events={this.props.events}/>
	      </div>
        <div className="ServiceBox--Body">
          <div className="TaskCounterHolder">
            <TaskCounter type="Desired" count={this.props.desiredCount} shouldBeZero={false}/>
            <TaskCounter type="Running" count={this.props.runningCount} shouldBeZero={false}/>
            <TaskCounter type="Pending" count={this.props.pendingCount} shouldBeZero={true}/>
          </div>
          <DeploymentList deployments={this.props.deployments}/>
        </div>
      </div>
    );
  }
}


export default ServiceBox;
