// @flow

import React, { Component } from 'react';
import type { Service } from './models'
import ServiceBox from './ServiceBox'
import './ServiceList.css';

class ServiceList extends Component {
  props: {
    serviceList: Array<Service>
  }

  compare(a: Service, b: Service) {
    return a.serviceName.localeCompare(
      b.serviceName
    )
  }

  render() {
      return <div className='ServiceList'>
          {this.props.serviceList.sort(this.compare).map(service =>
            <ServiceBox key={service.serviceName}
            {...service}
            />
          )}
      </div>
    }
}


export default ServiceList;
