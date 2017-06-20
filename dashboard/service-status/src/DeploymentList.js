// @flow

import React, { Component } from 'react';
import type { Deployment } from './models'
import './DeploymentList.css';

class DeploymentList extends Component {
  props: {
    deployments: Array<Deployment>
  }

  render() {
    const deployments = this.props.deployments;
    const ongoingDeployment = deployments.length > 1;

    return <div className={'DeploymentList ' + (ongoingDeployment ? 'DeploymentList__Deploying' : '')}>
      {deployments.map(deployment => {
        const taskDefinition = deployment.taskDefinition.split("/")[1]
        const taskVersion = (taskDefinition || "").split(":")[1]

        const classModifier = (() => { switch(deployment.status) {
          case "ACTIVE":
            return "Active";
          case "PRIMARY":
            return "Primary";
          case "INACTIVE":
            return "Inactive";
          default:
            return "Unknown";
        }})()

        const deploymentClassName = 'Deployment__' + classModifier;

        return <div className={'Deployment ' + deploymentClassName}  key={deployment.id}>
          {deployment.status}<span className="Deployment--Status">
            {taskVersion}
          </span>
        </div>
      })}
    </div>
  }
}


export default DeploymentList;
