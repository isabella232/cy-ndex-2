/**
 * main.js - The main server-side code for CyNDEx-2
*/

// For logging
const LOGGER = require('winston')

// For duplex communication via WebSocket
const WebSocket = require('ws')

// Required Electron classes
const {
  app,
  globalShortcut,
  BrowserWindow
} = require('electron')

// Constants are stored in this file
const {
  MSG_FOCUS,
  MSG_MINIMIZE,
  MSG_RESTORE,
  DESKTOP_SETTINGS
} = require('./config')

const WS_ADDRESS = 'ws://localhost:8025/ws/echo'

let ws // Web socket server
let mainWindow // The browser window for showing app.

// Flag for show/hide Chromium dev tools
let isDevEnabled = false

// Electron app is running or not
let isAppRunning = false

let block = false

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

    // Check CyREST port number
    let cyrestPortNumber = '1234'
    const argLen = process.argv.length

    if (argLen >= 3) {
      cyrestPortNumber = process.argv[2]
    }

    LOGGER.log('Got CyREST port from Cy3: ' + cyrestPortNumber)

    mainWindow.webContents.send('initialized', {
      cyrestPort: cyrestPortNumber
    })
  })

  // Load app from local directory.
  mainWindow.loadURL('http://localhost:2222/index.html')

  // Event handlers:
  initEventHandlers()
}

const initEventHandlers = () => {

  console.log('************************** Window Event')
  // Focus: the window clicked.
  mainWindow.on('focus', e => {
    if (mainWindow === null || mainWindow === undefined) {
      return
    }

    console.log('******** Got focus *************')
    if (!mainWindow.isDestroyed()) {
      mainWindow.setAlwaysOnTop(true);
      ws.send(JSON.stringify(MSG_FOCUS));
    }
  })

  // Window lost focus
  mainWindow.on('blur', e => {
    console.log('******** Lost focus *************')
    if (!mainWindow.isDestroyed()) {
      mainWindow.setAlwaysOnTop(false);
      mainWindow.hide()
    }
  })

  // Window minimized
  mainWindow.on('minimize', e => {
    e.preventDefault()
    ws.send(JSON.stringify(MSG_MINIMIZE));
  })

  // Restored from minimized state
  mainWindow.on('restore', e => {
    e.preventDefault()

    // TODO: Are there any better way to handle strange events from Windows system?
    setTimeout(() => {
      ws.send(JSON.stringify(MSG_RESTORE))
    }, 100)
  })

  // closed
  mainWindow.on('closed', () => {
    mainWindow = null
  })
}

const initSocket = () => {
  try {
    // Try Connection to server...
    ws = new WebSocket(WS_ADDRESS)

    ws.onopen = () => {
      // Start the app once WS connection is established
      initWindow()
    }

    // Listen for messages
    ws.onmessage = function (event) {
      let msgObj = JSON.parse(event.data)

      // Filter: ignore ndex messages
      if (msgObj.from === 'ndex') {
        return
      }

      switch (msgObj.type) {
        case 'minimized':
          if (mainWindow === undefined || mainWindow === null) {
            break
          }
          if (mainWindow.isMinimized() || block) {
            break
          }

          block = true
          mainWindow.minimize()
          block = false
          break

        case 'focus':
          if (mainWindow === undefined || mainWindow === null) {
            break
          }
          console.log("Focus from CY3------------")
          mainWindow.setAlwaysOnTop(true);
          setTimeout(() => {
            mainWindow.show()
          }, 20)

        case 'restored':
          if (mainWindow.isMinimized() && !block) {
            block = true
            mainWindow.restore()
            block = false
          }
          break
        default:
      }
    }

    ws.onclose = function () {
      if (mainWindow !== undefined && mainWindow !== null && !mainWindow.isDestroyed()) {
        mainWindow.close()
        mainWindow = null
      }
      app.quit()
    }

    setInterval(() => {
      const alive = {
        from: 'ndex',
        type: 'alive',
        body: 'NDEx Main alive'
      }
      ws.send(JSON.stringify(alive))
    }, 120000)
  } catch (e) {
    LOGGER.log('error', e);
  }
}

/**
 * A shortcut key combinations
 *
 */
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
})

// Quit the app when all windows are closed
app.on('window-all-closed', () => {
  app.quit()
})
