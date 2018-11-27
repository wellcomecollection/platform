// @flow

import React, { Component } from 'react';
import Moment from 'react-moment';
import './LastUpdated.css';

class LastUpdated extends Component {
  props: {
    time: Date
  }

  render() {
    return (
      <div className='LastUpdated'>
        Last updated: <Moment>{this.props.time}</Moment>
      </div>
    );
  }
}


export default LastUpdated;
