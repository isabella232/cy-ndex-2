/**
  main.js - The main server-side code for CyNDEx-2
*/

// Electron app logging
const LOGGER = require('winston');

// For duplex communication via WebSocket
const WebSocket = require('ws');

// Required Electron classes
const {
  app,
  globalShortcut,
  BrowserWindow,
} = require('electron')

// Constants are stored in this file
const {
  MSG_FOCUS,
  MSG_MINIMIZE,
  MSG_RESTORE,
  DESKTOP_SETTINGS
} = require('./config')


const WS_ADDRESS = 'ws://localhost:8025/ws/echo';

let ws // Web socket server
let mainWindow // The browser window for showing app.

let block = false;
let minimized = false;
let isDevEnabled = false;
let isAppRunning = false;

/**
  Initialize logger
*/
const initLogger = () => {
  LOGGER.add(LOGGER.transports.File, {
    filename: 'cyndex-2.log'
  })
  LOGGER.level = 'debug'
}


/**
  Initialize the application window
*/
const initWindow = () => {
  // Create the browser window.
  mainWindow = new BrowserWindow(DESKTOP_SETTINGS)

  // Send settings to the renderer
  mainWindow.webContents.on('did-finish-load', () => {

    // Check port number
    let cyrestPortNumber = '1234'
    const argLen = process.argv.length

    if(argLen >= 3) {
      cyrestPortNumber = process.argv[2]
    }

    LOGGER.log('debug', 'Got CyREST port from Cy3: ' + cyrestPortNumber)
    mainWindow.webContents.send('initialized', {
      cyrestPort: cyrestPortNumber
    })
  })

  // Load app from local directory.
  const dir = `${__dirname}`
  mainWindow.loadURL('file://' + dir + '/webapp/ndex/index.html')

  // Event handlers:
  initEventHandlers()
};


const initEventHandlers = () => {

  // Focus: the window clicked.
  mainWindow.on('focus', (e) => {

    if (block || mainWindow === null || mainWindow === undefined) {
      return
    }

    if (!mainWindow.isDestroyed() && !mainWindow.isAlwaysOnTop()) {
      mainWindow.setAlwaysOnTop(true);
      ws.send(JSON.stringify(MSG_FOCUS));

      setTimeout(() => {
        mainWindow.setAlwaysOnTop(false);
      }, 200);
    }
  });

  // Window minimized
  mainWindow.on('minimize', (e) => {
    e.preventDefault();
    ws.send(JSON.stringify(MSG_MINIMIZE));
  });

  // Restored from minimized state
  mainWindow.on('restore', (e) => {
    e.preventDefault()

    // TODO: Are there any better way to handle strange events from Windows system?
    setTimeout(() => {
      ws.send(JSON.stringify(MSG_RESTORE));
    }, 200);
  })

  // closed
  mainWindow.on('closed', () => { mainWindow = null })
}

const initSocket = () => {
  try {
    // Try Connection to server...
    ws = new WebSocket(WS_ADDRESS)

    ws.onopen = () => {
      // Start the app once WS connection is established
      initWindow({});
    };

    // Listen for messages
    ws.onmessage = function(event) {
      let msgObj = JSON.parse(event.data);

      // Filter: ignore ndex messages
      if (msgObj.from === 'ndex') {
        return;
      }

      switch (msgObj.type) {
        case 'minimized':
          if (mainWindow === undefined || mainWindow === null) {
            break;
          }

          if (mainWindow.isMinimized() || block) {
            break;
          }

          block = true;
          mainWindow.minimize();
          block = false;
          break;

        case 'focus':
          if (mainWindow === undefined || mainWindow === null) {
            break;
          }
          if (mainWindow.isFocused() || block) {
            break;
          }

          block = true;

          try {
            if (!mainWindow.isAlwaysOnTop()) {
              mainWindow.setAlwaysOnTop(true);
              mainWindow.showInactive();
              setTimeout(() => {
                mainWindow.setAlwaysOnTop(false);
              }, 550);
            }

            block = false;
            break;

          } catch (ex) {
            LOGGER.log('error', ex)
          }
        case 'restored':
          if (mainWindow.isMinimized() && !block) {
            block = true;
            mainWindow.restore();
            block = false;
          }
          break;
        default:
      }
    };

    ws.onclose = function() {
      if (mainWindow !== undefined && mainWindow !== null && !mainWindow.isDestroyed()) {
        mainWindow.close()
        mainWindow = null
      }
      app.quit()
    };

    setInterval(function() {
      const alive = {
        from: 'ndex',
        type: 'alive',
        body: 'NDEx Main alive'
      }
      ws.send(JSON.stringify(alive));
    }, 120000);
  } catch (e) {
    console.log(e);
    LOGGER.log('error', e);
  }
}

const addShortcuts = () => {

  globalShortcut.register('CommandOrControl+w', () => {
    app.quit()
  })

  // For dev tool
  globalShortcut.register('CommandOrControl+d', () => {
    if (isDevEnabled) {
      mainWindow.webContents.closeDevTools();
      isDevEnabled = false;
    } else {
      mainWindow.webContents.openDevTools();
      isDevEnabled = true;
    }
  })
}


////////////////////////////////////////////////////////////////////////////////
// Start the app
////////////////////////////////////////////////////////////////////////////////

app.on('ready', () => {
  if (isAppRunning) {
    return
  }
  initLogger()
  initSocket()

  addShortcuts()
  isAppRunning = true
});

// Quit the app when all windows are closed
app.on('window-all-closed', () => {
  app.quit()
})
