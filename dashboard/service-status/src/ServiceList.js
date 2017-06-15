// @flow

import React, { Component } from 'react';
import type { Services } from './models'
import ServiceBox from './ServiceBox'
import './ServiceList.css';

class ServiceList extends Component {
  props: {
    services: Services,
  }

  render() {
    return (
      <div className='ServiceList'>
        {this.props.services.map(service =>
          <ServiceBox key={service.serviceName}
          {...service}
          />
        )}
      </div>
    );
  }
}


export default ServiceList;
