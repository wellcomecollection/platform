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
    const isZero = this.props.count === 0;
    const shouldBeZero = this.props.shouldBeZero;
    const notifyState = isZero && shouldBeZero;
    const correctState = notifyState || (!isZero && !shouldBeZero);

    const stateModifier = (() => {
      if(!notifyState) {
        if(correctState){
          return 'Safe';
        } else {
          return 'Warn';
        }
      } else {
        return 'Ignore';
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
