// Logging
const LOGGER = require('winston');

const APP_NAME_VALET = 'ndex';
const APP_NAME_SAVE = 'ndex-save';
const APP_NAME_LOGIN = 'ndex-login';

const APP_CONFIG_VALET = require('./webapp/ndex/config');
const APP_CONFIG_SAVE = require('./webapp/ndex-save/config');
const APP_CONFIG_LOGIN = require('./webapp/ndex-login/config');

const APP_CONFIG_MAP = new Map();
APP_CONFIG_MAP.set(APP_NAME_VALET, APP_CONFIG_VALET);
APP_CONFIG_MAP.set(APP_NAME_SAVE, APP_CONFIG_SAVE);
APP_CONFIG_MAP.set(APP_NAME_LOGIN, APP_CONFIG_LOGIN);

// Required Electron components
const { app, globalShortcut, BrowserWindow, Menu } = require('electron');

global.sharedObj = { temp: app.getPath('temp') };
console.log(global.sharedObj);

// For duplex communication
const WebSocket = require('ws');

// TODO: make this injectable
const WS_ADDRESS = 'ws://localhost:8025/ws/echo';

let ws;
let mainWindow;
let opts;

// Type of the app for this instance.
let appName = null;

let block = false;
let isDevEnabled = false;


const MSG_SELECT_APP = {
  from: 'ndex',
  type: 'app',
  body: ''
};

const MSG_FOCUS = {
  from: 'ndex',
  type: 'focus',
  body: 'Ndex focused'
};

const MSG_SAVE = {
  from: 'ndex',
  type: 'save',
  body: ''
};


function initLogger() {
  LOGGER.add(LOGGER.transports.File, { filename: 'electron-app.log' });
  LOGGER.level = 'debug';
  LOGGER.log('debug', 'Starting app');
}

function initWindow(appType) {
  // Create the browser window.
  LOGGER.log('debug', 'Target app = ' + appType);
  appName = appType;

  mainWindow = new BrowserWindow(APP_CONFIG_MAP.get(appType));
  mainWindow.webContents.on('did-finish-load', () => {
    mainWindow.webContents.send('ping', opts);
  });

  const dir = `${__dirname}`;
  global.sharedObj.dir = dir;

  mainWindow.loadURL('file://' + dir + '/webapp/' + appType + '/index.html');

  // Event handlers:

  // Emitted when the window is closed.
  mainWindow.on('focus', () => {
    if (block || mainWindow === null || mainWindow === undefined) {
      return;
    }

    if(!mainWindow.isDestroyed() && !mainWindow.isAlwaysOnTop()) {
      mainWindow.setAlwaysOnTop(true);
      mainWindow.show();

      ws.send(JSON.stringify(MSG_FOCUS));

      setTimeout(()=> {
        mainWindow.setAlwaysOnTop(false);
        console.log('DISABLE Always on top: ----------- ')
      }, 300);
    }
  });


  if (appType === APP_NAME_SAVE) {
    initSave();
  }
}

function initSave() {
  ws.send(JSON.stringify(MSG_SAVE));
}

function initSocket() {
  try {
    // Try Connection to server...
    ws = new WebSocket(WS_ADDRESS);

    ws.onopen = () => {
      ws.send(JSON.stringify(MSG_SELECT_APP));
    };

    // Listen for messages
    ws.onmessage = function (event) {
      let msgObj = JSON.parse(event.data);

      LOGGER.log("debug", '$$$$$$$$MSG: ');
      LOGGER.log('debug', msgObj);

      // Filter: ignore ndex messages
      if (msgObj.from === "ndex") {
        return;
      }

      switch (msgObj.type) {
        case 'app':
          LOGGER.log("debug", "==== APP Type Message ====");
          LOGGER.log("debug", msgObj);
          opts = msgObj.options;
          initWindow(msgObj.body);
          break;
        case 'minimized':
          if (mainWindow === undefined || mainWindow === null) {
            break;
          }

          console.log('######## MINIMIZE request')
          mainWindow.minimize();
          break;

        case "focus":
          if(mainWindow === undefined || mainWindow === null) {
              break;
          }

          console.log('######## Focus request: ----------- ')
          block = true;

          try {


            if(!mainWindow.isAlwaysOnTop()) {
              console.log('######## ALWAYS on top')
              mainWindow.setAlwaysOnTop(true);
              mainWindow.showInactive();
              setTimeout(()=> {
                mainWindow.setAlwaysOnTop(false);
              }, 250);
            }

            var msg = {
              from: "ndex",
              type: "focus-success",
              body: "Ndex focuse Success"
            };

            ws.send(JSON.stringify(msg));

            block = false;
            break;

          } catch(ex) {

          }
        case "save":
          opts = msgObj.options;
          // LOGGER.log("debug", 'Fire2: Got Save Params: ' + opts);
          // mainWindow.setTitle('Save to NDEx: ' + opts.name);
          break;
      }
    };

    ws.onclose = function () {
      if(mainWindow !== undefined && mainWindow !== null && !mainWindow.isDestroyed()) {
        mainWindow.close()
        mainWindow = null
      }

      app.quit()
    };

    setInterval(function() {
      "use strict";
      let alive = {
        from: "ndex",
        type: "alive",
        body: "NDEx Main alive"
      };

      ws.send(JSON.stringify(alive));
    }, 120000);

  } catch (e) {
    console.log(e);
  }
}


function createWindow() {
  initLogger();
  // Establish WS connection
  initSocket();
}


function addShortcuts() {
  globalShortcut.register('CommandOrControl+w', function () {
    console.log('Close (w) is pressed');
    app.quit()
  });

  // For dev tool
  globalShortcut.register('CommandOrControl+d', function () {
    console.log('Devtool');
    if (isDevEnabled) {
      mainWindow.webContents.closeDevTools();
      isDevEnabled = false;
    } else {
      mainWindow.webContents.openDevTools();
      isDevEnabled = true;
    }
  });
}


app.on('ready', () => {

  // Respond to only once.  One app == one instance.
  if(appName === null) {
    createWindow();
    addShortcuts();
  }
});


// Quit when all windows are closed.
app.on('window-all-closed', function () {
  app.quit()
});
