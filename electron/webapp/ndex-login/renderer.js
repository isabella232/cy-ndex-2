const remote = require('electron').remote;
const dialog = require('electron').remote.dialog;
const WebSocket = require('ws');
const {ipcRenderer} = require('electron');

const WS_SERVER = 'ws://localhost:8025/ws/echo';

const win = remote.getCurrentWindow();

const DEF_SERVER = 'http://public.ndexbio.org';
const DEF_NAME = 'NDEx Public';
const AUTH_API = '/rest/user/authenticate';

const DEFAULTS = {
  userName: '',
  userPass: '',
  serverName: DEF_NAME,
  serverAddress: DEF_SERVER,
  loggedIn: false
}

const LOGIN = {
  from: 'ndex',
  type: 'login',
  body: '',
  options: {}
};

const MSG_ERROR = {
  title: 'Error:',
  type: 'info',
  buttons: ['Retry'],
  message: 'Failed to login, ',
  detail: 'Please check your inputs and try again.'
}

const MSG_SUCCESS = {
  title: 'NDEx Replied:',
  type: 'info',
  buttons: ['OK'],
  message: 'Welcome Back, ',
  detail: 'Status: Login Success'
}

// Get options from main process
ipcRenderer.on('ping', (event, arg) => {
  let loginInfo = arg;

  if (loginInfo === undefined || loginInfo === null || loginInfo === {}) {
    loginInfo = DEFAULTS;
  }

  // Login is ready.  Init opts
  init(loginInfo);
})


let ws = null;


function init(loginInfo) {
  const cyto = CyFramework.config([NDExStore]);

  cyto.render(NDExLogin, document.getElementById('valet'), {
    defaults: loginInfo,

    onSubmit: () => {
      const state = cyto.getStore('ndex').server.toJS();
      connect(state);
    },

    onLogout: () => {
      console.log('LOGOUT!!!')
      logout()
    }
  });

  ws = new WebSocket(WS_SERVER);
}

function logout() {
  sendMessage(null);
}


function connect(credential) {
  const id = credential.userName;
  const pw = credential.userPass;
  let serverUrl = credential.serverAddress;

  if (serverUrl === undefined || serverUrl === '') {
    serverUrl = DEF_SERVER;
  }

  if (credential.serverName === undefined || credential.serverName === '') {
    credential.serverName = DEF_NAME;
  }

  // Add API addition
  credential.serverAddress = serverUrl;
  serverUrl = serverUrl + AUTH_API;

  const q = {
    method: 'get',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      Authorization: 'Basic ' + btoa(id + ':' + pw)
    }
  };

  fetch(serverUrl, q)
    .then(response => {
      if (response.ok) {
        return response.json();
      } else {
        dialog.showMessageBox(win, MSG_ERROR);
      }
    })
    .then(json => {
      const msg = MSG_SUCCESS;
      msg.message = msg.message + json.firstName;
      dialog.showMessageBox(win, msg, () => {
        sendMessage(credential);
      });
    });
}


function sendMessage(credential) {
    const loginSuccess = LOGIN;
    loginSuccess.options = credential;
    ws.send(JSON.stringify(loginSuccess));
    ws.close();
    win.close();
}