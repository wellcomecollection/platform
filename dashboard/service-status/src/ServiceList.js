// @flow

import React, { Component } from 'react';
import type { Services } from './models'
import ServiceBox from './ServiceBox'


class ServiceList extends Component {
  props: {
    services: Services,
  }

  render() {
    return (
      <div className="Services">
        {this.props.services.map(service =>
          <ServiceBox
          {...service}
          />
        )}
      </div>
    );
  }
}


export default ServiceList;
