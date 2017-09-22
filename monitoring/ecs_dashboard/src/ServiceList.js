// @flow

import React, { Component } from 'react';
import type { Service } from './models'
import ServiceBox from './ServiceBox'
import './ServiceList.css';

class ServiceList extends Component {
  props: {
    serviceList: Array<Service>
  }

  render() {
      return <div className='ServiceList'>
          {this.props.serviceList.map(service =>
            <ServiceBox key={service.serviceName}
            {...service}
            />
          )}
      </div>
    }
}


export default ServiceList;
