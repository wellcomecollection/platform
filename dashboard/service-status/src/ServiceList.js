// @flow

import React, { Component } from 'react';
import type { Services } from './models'
import ServiceBox from './ServiceBox'
import ErrorBox from './ErrorBox'
import './ServiceList.css';

class ServiceList extends Component {
  props: {
    services: Services,
    isError: boolean,
    error: ?Error
  }

  render() {
      if(this.props.isError) {
        return <ErrorBox error={this.props.error}/>
      } else {
        return <div className='ServiceList'>
          {this.props.services.map(service =>
            <ServiceBox key={service.serviceName}
            {...service}
            />
          )}
        </div>
    }
  }
}


export default ServiceList;
