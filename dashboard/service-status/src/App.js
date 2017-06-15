import React, { Component } from 'react';
import VisibleServiceList from './VisibleServiceList';
import VisibleLastUpdated from './VisibleLastUpdated'
import './App.css';

class App extends Component {
  render() {
    return (
      <div className="App">
        <div className="App-header">
          <h2>Service Status</h2>
        </div>
        <VisibleServiceList />
        <VisibleLastUpdated />
      </div>
    );
  }
}

export default App;
