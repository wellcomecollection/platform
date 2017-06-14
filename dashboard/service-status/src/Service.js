// @flow

import React, { Component } from 'react';

class Service extends Component {
  props: {
    name: string
  }

  render() {
    return (
      <div className="Service">
        <p className="Service-Name">
	        My Service: {this.props.name}
	      </p>
      </div>
    );
  }
}


export default Service;
