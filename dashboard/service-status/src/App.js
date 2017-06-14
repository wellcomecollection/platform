import React, { Component } from 'react';
import VisibleService from './VisibleService';
import './App.css';

class App extends Component {
  render() {
    return (
      <div className="App">
        <div className="App-header">
          <h2>Welcome to Zombocom</h2>
        </div>
        <VisibleService />
      </div>
    );
  }
}

export default App;
