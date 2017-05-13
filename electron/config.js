/**
  WS messages for window state management
*/
const MSG_FOCUS = {
  from: 'ndex',
  type: 'focus',
  body: 'Ndex focused'
}

const MSG_MINIMIZE = {
  from: 'ndex',
  type: 'minimized',
  body: 'Ndex window minimized'
}

const MSG_RESTORE = {
  from: 'ndex',
  type: 'restored',
  body: 'Ndex window restored'
}

/**
  Basic setup for the electron window
*/
const DESKTOP_SETTINGS = {
  width: 900,
  height: 750,
  minHeight: 400,
  minWidth: 500,
  frame: true,
  title: 'CyNDEx-2',
  alwaysOnTop: false,
  minimizable: false,
  maximizable: false,
  fullscreenable: false
}

module.exports.MSG_RESTORE = MSG_RESTORE
module.exports.MSG_MINIMIZE = MSG_MINIMIZE
module.exports.MSG_FOCUS = MSG_FOCUS

module.exports.DESKTOP_SETTINGS = DESKTOP_SETTINGS
