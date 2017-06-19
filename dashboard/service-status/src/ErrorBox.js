// @flow

import React, { Component } from 'react';
import './ErrorBox.css'

class ErrorBox extends Component {
  props: {
    error: Error,
  }

  render() {

    console.log(this.props);
    return (
      <div className='ErrorMessage'>
        <em>Something went wrong:</em> <strong>{this.props.error.message}</strong>
      </div>
    );
  }
}


export default ErrorBox;
