import React, { Component } from 'react';
import VisibleServiceList from './VisibleServiceList';
import './App.css';

class App extends Component {
  render() {
    return (
      <div className="App">
        <div className="App-header">
          <h2>Service List</h2>
        </div>
        <VisibleServiceList />
      </div>
    );
  }
}

export default App;
