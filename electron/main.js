const { app, globalShortcut, BrowserWindow } = require('electron');
const {MSG_FOCUS, MSG_MINIMIZE, MSG_RESTORE, DESKTOP_SETTINGS} = require('./config')

// Electron app logging
const LOGGER = require('winston');

// TODO: Add logging config

// Required Electron components

// Remove this...
global.sharedObj = { temp: app.getPath('temp') };
console.log(global.sharedObj);

// Current electron dir
const dir = `${__dirname}`;
global.sharedObj.dir = dir;

// For duplex communication
const WebSocket = require('ws');

// TODO: make this injectable
const WS_ADDRESS = 'ws://localhost:8025/ws/echo';

let ws; // Web socket server
let mainWindow; // The browser window for showing app.
let opts;


let block = false;
let minimized = false;
let isDevEnabled = false;
let isAppRunning = false;


// Basic setup for the application

const initLogger = () => {
  LOGGER.add(LOGGER.transports.File, { filename: 'cyndex-2.log' });
  LOGGER.level = 'debug';
}

/**
  Initialize the application window
*/
const initWindow = () => {
  // Create the browser window.
  mainWindow = new BrowserWindow(DESKTOP_SETTINGS);
  mainWindow.webContents.on('did-finish-load', () => {
    mainWindow.webContents.send('ping', opts);
  });

  const url = global.sharedObj.url;
  console.log('## Opening: ' + url)

  if(url === undefined || url === null) {
    mainWindow.loadURL('file://' + dir + '/webapp/ndex/index.html');
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


const initSocket = () => {
  try {
    // Try Connection to server...
    ws = new WebSocket(WS_ADDRESS);

    ws.onopen = () => {

      // Start the app once WS connection is established
      initWindow({});
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
    LOGGER.log('error', e);
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

  createWindow();
  addShortcuts();
  isAppRunning = true;
});

// Quit when all windows are closed.
app.on('window-all-closed', function () {
  app.quit()
});
