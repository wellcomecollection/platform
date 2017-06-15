// @flow

import React, { Component } from 'react';
import './TaskCounter.css'

class TaskCounter extends Component {
  props: {
    type: string,
    count: number,
    shouldBeZero: boolean
  }

  render() {
    const nonZero = this.props.count > 0
    const happyState = (nonZero && !this.props.shouldBeZero) || (!nonZero && this.props.shouldBeZero)
    const notifyState = happyState && nonZero

    const stateModifier = (() => {
      if(happyState && notifyState) {
        return 'Safe'
      } else if (!happyState && notifyState) {
        return 'Warn'
      } else {
        return 'Ignore'
      }
    })()

    return (
      <div className={'TaskCounter TaskCounter__' + stateModifier}>
        <div className="TaskCounter--Type">{this.props.type}</div>
        <div className="TaskCounter--Count">{this.props.count}</div>
      </div>
    );
  }
}


export default TaskCounter;
