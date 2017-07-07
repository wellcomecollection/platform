import React, { Component } from 'react';
import VisibleClusterList from './VisibleClusterList';
import VisibleLastUpdated from './VisibleLastUpdated'
import './App.css';

class App extends Component {
  render() {
    return (
      <div className="App">
        <div className="App-header">
          <h2>Service Status</h2>
        </div>
        <div className="App-body">
          <VisibleClusterList />
          <VisibleLastUpdated />
        </div>
      </div>
    );
  }
}

export default App;
