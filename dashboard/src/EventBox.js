// @flow

import React, { Component } from 'react';
import type { Event } from './models'
import './EventBox.css';

class EventBox extends Component {
  props: {
    events: Array<Event>
  }

  render() {
      return <div className="EventBox">
        <div className="EventList">
          {this.props.events.reverse().map(event => {
            return <div className="EventList--Event" key={event.timestamp}>&gt; {event.message}</div>
          })}
          <div className="EventList--Event">&gt;</div>
        </div>
      </div>
    }
}


export default EventBox;
