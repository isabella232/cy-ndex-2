// Logging
const LOGGER = require('winston');

const APP_NAME_VALET = 'ndex';
const APP_NAME_SAVE = 'ndex-save';

const APP_CONFIG_VALET = require('./webapp/ndex/config');

const APP_CONFIG_MAP = new Map();
APP_CONFIG_MAP.set(APP_NAME_VALET, APP_CONFIG_VALET);


// Required Electron components
const { app, globalShortcut, BrowserWindow } = require('electron');

global.sharedObj = { temp: app.getPath('temp') };
console.log(global.sharedObj);

// Current dir
const dir = `${__dirname}`;
global.sharedObj.dir = dir;

// URL to be opened for the specified app
const APP_URLS = new Map();
APP_URLS.set(APP_NAME_SAVE, 'file://' + dir + '/webapp/ndex/save.html')


// For duplex communication
const WebSocket = require('ws');

// TODO: make this injectable
const WS_ADDRESS = 'ws://localhost:8025/ws/echo';

let ws; // Web socket server
let mainWindow; // The browser window for showing app.

let opts;

// Type of the app for this instance.
let appName = null;

let block = false;

let minimized = false;
let isDevEnabled = false;

let isAppRunning = false;


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

const MSG_MINIMIZE = {
  from: 'ndex',
  type: 'minimized',
  body: 'Ndex window minimized'
};

const MSG_RESTORE = {
  from: 'ndex',
  type: 'restored',
  body: 'Ndex window restored'
};

const MSG_SAVE = {
  from: 'ndex',
  type: 'save',
  body: ''
};


const initLogger = () => {
  LOGGER.add(LOGGER.transports.File, { filename: 'electron-app.log' });
  LOGGER.level = 'debug';
  LOGGER.log('debug', 'Starting app');
}

const initWindow = appType => {
  // Create the browser window.
  LOGGER.log('debug', 'Target app = ' + appType);
  appName = appType;

  mainWindow = new BrowserWindow(APP_CONFIG_MAP.get(appType));
  mainWindow.webContents.on('did-finish-load', () => {
    mainWindow.webContents.send('ping', opts);
  });


  const url = APP_URLS.get(appType)
  if(url === undefined || url === null) {
    mainWindow.loadURL('file://' + dir + '/webapp/' + appType + '/index.html');
  } else {
    mainWindow.loadURL(url);
  }

  // Event handlers:
  initEventHandlers();
};


const initEventHandlers = () => {

  // Focus: the window clicked.
  mainWindow.on('focus', (e) => {
    console.log("event restore:")
    if (block || mainWindow === null || mainWindow === undefined) {
      return;
    }

    if(!mainWindow.isDestroyed() && !mainWindow.isAlwaysOnTop()) {
      mainWindow.setAlwaysOnTop(true);

      ws.send(JSON.stringify(MSG_FOCUS));

      setTimeout(()=> {
        mainWindow.setAlwaysOnTop(false);
        console.log('DISABLE Always on top: ----------- ')
      }, 200);
    }
  });

  mainWindow.on('minimize', (e) => {
    e.preventDefault();
    ws.send(JSON.stringify(MSG_MINIMIZE));

  });

  mainWindow.on('restore', (e) => {
    e.preventDefault();

    // TODO: Are there any better way to handle strange events from Windows system?
    setTimeout(()=> {
        ws.send(JSON.stringify(MSG_RESTORE));
    }, 200);
  });

};


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

          if(mainWindow.isMinimized() || block) {
            break;
          }

          console.log('######## MINIMIZE request')
          block = true;
          mainWindow.minimize();
          block = false;
          break;

        case "focus":
          if(mainWindow === undefined || mainWindow === null) {
              break;
          }
          if(mainWindow.isFocused() || block) {
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
              }, 550);
            }

            block = false;
            break;

          } catch(ex) {

          }
        case 'restored':
          console.log('######## RESTORE request: ----------- ')
          if(mainWindow.isMinimized() && !block) {
            block = true;
            mainWindow.restore();
            block = false;
          }
          break;
        default:
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

  if(isAppRunning) {
    return;
  }

  // Respond to only once.  One app == one instance.
  if(appName === null) {
    createWindow();
    addShortcuts();
    isAppRunning = true;
  }
});

// Quit when all windows are closed.
app.on('window-all-closed', function () {
  app.quit()
});
