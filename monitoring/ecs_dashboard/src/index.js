import React from 'react';
import ReactDOM from 'react-dom';
import App from './App';
import './index.css';

import { Provider } from 'react-redux'
import { fetchServices } from './actions'

import rootReducer from './reducers'
import thunkMiddleware from 'redux-thunk'
import { createLogger } from 'redux-logger'
import { createStore, applyMiddleware } from 'redux'

const loggerMiddleware = createLogger()

const Store = createStore(
  rootReducer,
  applyMiddleware(
    thunkMiddleware, // lets us dispatch() functions
    loggerMiddleware // neat middleware that logs actions
  )
)

ReactDOM.render(
  <Provider store={Store}>
    <App />
  </Provider>
  ,
  document.getElementById('root')
);

Store.dispatch(fetchServices());

function doWork(callback) {
  setTimeout(function() {
    Store.dispatch(fetchServices())
    doWork();
  }, 5000);
}

doWork();
