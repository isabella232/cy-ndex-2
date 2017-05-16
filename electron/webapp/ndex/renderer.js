/**
  Basic template code for initializing an Electron app
*/

// This provides access to the Electron node.js server
const remote = require('electron').remote

// Get data from node.js server process
const {
  ipcRenderer
} = require('electron')

const {
  BrowserWindow,
  Menu,
  MenuItem
} = require('electron').remote

/**
  Adding menu items to context menu (right-click)
*/
const initContextMenu = () => {
  const menu = new Menu()
  menu.append(new MenuItem({
    label: 'Cut',
    type: 'normal',
    role: 'cut'
  }))

  menu.append(new MenuItem({
    label: 'Copy',
    type: 'normal',
    role: 'copy'
  }))

  menu.append(new MenuItem({
    label: 'Paste',
    type: 'normal',
    role: 'paste'
  }))

  menu.append(new MenuItem({
    label: 'Select all',
    type: 'normal',
    role: 'selectall'
  }))

  window.addEventListener('contextmenu', e => {
    e.preventDefault()
    menu.popup(remote.getCurrentWindow())
  }, false)
}



const getAppState = cyrestPort => {

  const url = 'http://localhost:' + cyrestPort + '/cyndex2/v1/status'
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json'
  }

  const param = {
    method: 'get',
    headers: headers
  };

  fetch(url, param)
    .then(response => (response.json()))
    .then(json => {
      console.log(json)

      document.getElementById('appType').innerHTML = 'Widget Type: ' + json.data.widget
      remote.getCurrentWindow().setTitle('CyNDEx-2: ' + json.data.widget)
    })
}

const init = cyrestPort => {

  // Here is how to add close button in the app
  const close = document.getElementById('close')
  close.onclick = () => {
    remote.getCurrentWindow().close();
  }
  getAppState(cyrestPort)
}

// Get options from main process and start the app
ipcRenderer.on('initialized', (event, params) => {
  // This contains CyREST port number
  const cyrestPort = params.cyrestPort
  document.getElementById('cyrest').innerHTML = 'CyREST Port Number: ' + cyrestPort

  // Start the app
  init(cyrestPort)
})
