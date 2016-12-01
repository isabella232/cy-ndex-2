const remote = require('electron').remote;
const dialog = require('electron').remote.dialog;
const {ipcRenderer} = require('electron');
const {BrowserWindow, Menu, MenuItem} = require('electron').remote;

const fs = require('fs')
const os = require('os')
const path = require('path')
const request = require('request');



const tempDir = os.tmpdir()

// For posting to Minio server
const Minio = require('minio');

const IMAGE_SERVER = 'storage.cytoscape.io';
const minioClient = new Minio(
  {
    endPoint: IMAGE_SERVER,
    secure: false
  }
);



// Main browser window
const win = remote.getCurrentWindow();

// For loading animation
const child = new BrowserWindow({
  parent: win, modal: true, show: false,
  width: 400, height: 400
});

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

window.addEventListener('contextmenu', (e) => {
  e.preventDefault()
  menu.popup(remote.getCurrentWindow())
}, false)



const RESERVED_TAGS = [
  "author",
  "creationTime",
  "description",
  "disease",
  "edgeCount",
  "modificationTime",
  "nodeCount",
  "organism",
  "rights",
  "rightsHolder",
  "tissue",
  "uuid",
  "version",
  "visibility"
];

const UUID_TAG = 'ndex:uuid'


const THEME = {
  palette: {
    primary1Color: '#6E93B6',
    primary2Color: '#244060',
    primary3Color: '##EDEDED',
    accent1Color: '#D69121',
    accent2Color: '#E4E4E4',
    accent3Color: '##9695A6'
  }
};

const PRESET_PROPS = [
  'networkName', 'private',
  'author', 'organism',
  'disease', 'tissue',
  'rightsHolder', 'rights',
  'reference','description'
]

// Color theme of the UI

const STYLE = {
  backgroundColor: '#EDEDED'
}


const HEADERS = {
  'Accept': 'application/json',
  'Content-Type': 'application/json'
}

const MSG_ERROR = {
  title: 'Save Error:',
  type: 'error',
  buttons: ['Close'],
  message: 'Failed to save file, ',
  detail: 'Failed.'
};

const MSG_ERROR_LOGIN = {
  title: 'Login Error',
  type: 'error',
  buttons: ['Close'],
  message: 'Failed to Login:',
  detail: 'You MUST login before saving networks.'
};

const MSG_ERROR_NOT_FOUND = {
  title: 'No such network',
  type: 'error',
  buttons: ['Close'],
  message: 'Network does not exist:',
  detail: 'Failed to find the existing entry in NDEx server.' +
  'New record will be created for this collection.'
};

const MSG_ERROR_NOT_OWNER = {
  title: 'Permission Error',
  type: 'warning',
  buttons: ['Close'],
  message: 'You are not the owner of this network:',
  detail: 'Since you are not the owner of this network, new record will be created.'
};

const MSG_ERROR_IMAGE_SAVE = {
  title: 'Network Saved, but image upload failed',
  type: 'warning',
  buttons: ['Close'],
  message: 'Saved, but no image:',
  detail: 'Network was successfully saved, ' +
  'but could not upload network image. Possibly an image cache server problem.'
};

const MSG_ERROR_CYREST = {
  title: 'Import Error:',
  type: 'error',
  buttons: ['Close'],
  message: 'Could not get properties from session',
  detail: 'Failed.'
};


// Default parameters (credentials for the application)
let options;

let isPrivate = true
let overwrite = false
let existingUuid = null


function fillForm(table) {
  const row = table.rows[0];
  existingUuid = row[UUID_TAG]

  if(existingUuid !== undefined &&
      existingUuid !== null && existingUuid !== '') {
    checkUuid(existingUuid)
  }

  return {
    UUID: row[UUID_TAG],
    networkName: row.name,
    private: true,
    author: row.author,
    organism: row.organism,
    disease: row.disease,
    tissue: row.tissue,
    rightsHolder: row.rightsHolder,
    rights: row.rights,
    reference: row.reference,
    description: row.description,
    theme: THEME,
    style: STYLE,

    onSave(newProps) {
      isPrivate = newProps.private
      overwrite = newProps.overwrite
      showLoading()
      deleteInvalidColumns(options.rootSUID, newProps)
    }
  }
}


/**
 * Import Collection table from Cytoscape
 */
function getTable() {

  const params = {
    method: 'get',
    headers: HEADERS
  }

  const url = 'http://localhost:' + options.cyrestPort +'/v1/collections/' + options.rootSUID + '/tables/default'

  fetch(url, params)
    .then(response => {
      if (response.ok) {
        return response.json();
      } else {
        dialog.showMessageBox(win, MSG_ERROR_CYREST, () => {
          win.close()
        });
      }
    })
    .then(json => {
      win.setTitle('Save a Collection of Networks: ' + options.rootname);
      init(json);
    });
}


function createUpdateTable(props, suid) {
  const params = {
    key: 'SUID',
    dataKey: 'SUID',
    data: []
  };

  const entry = {
    SUID: parseInt(suid, 10)
  };

  for (let key of PRESET_PROPS) {
    const val = props[key];
    if (val !== undefined && val !== null && val !== '') {
        if(key === 'networkName') {
          entry['name'] = val;
        } else {
          entry[key] = val;
        }
    }
  }
  params.data.push(entry);

  return params;
}


/**
 * Update the network table of the Collection
 *
 * @param rootSuid
 * @param newProps
 * @returns {*}
 */
function updateRootTable(rootSuid, newProps) {
  const data = createUpdateTable(newProps, rootSuid);
  const params = {
    method: 'put',
    headers: HEADERS,
    body: JSON.stringify(data)
  }

  const url = 'http://localhost:' + options.cyrestPort + '/v1/collections/' + rootSuid + '/tables/default';
  fetch(url, params)
    .then(postCollection());
}

function deleteInvalidColumns(rootSuid, newProps) {
  const params = {
    method: 'delete',
    headers: HEADERS,
  }

  // Get list of subnetworks in this collection
  const url = 'http://localhost:' + options.cyrestPort +'/v1/collections/' + rootSuid + '/tables/shared/columns/';

  Promise.all(RESERVED_TAGS.map(tag => {
    const removeUrl = url + tag
    return fetch(removeUrl, params)
  })).then(() => {
    updateRootTable(rootSuid, newProps)
  })
}



function init(table) {
  const cyto = CyFramework.config([NDExStore])
  const params = fillForm(table)
  cyto.render(NDExSave, document.getElementById('save'), params);
}


function addCloseButton() {
  document.getElementById('close').addEventListener('click', () => {
    win.close();
  });
}


function startApp() {
  addCloseButton();
  getTable();
}


function postCx(rawCX) {

  // For checking performance
  let start = null;

  const ndexServerAddress = options.serverAddress;
  const id = options.userName;
  const pass = options.userPass;
  let isUpdate = false

  let url = ndexServerAddress + '/rest/network/asCX';
  if(existingUuid !== null
      && existingUuid !== undefined && overwrite === true) {
    url = url + '/' + existingUuid
    isUpdate = true
    console.log("* Will update existing entry")
  }

  const XHR = new XMLHttpRequest();
  const FD = new FormData();
  const content = JSON.stringify(rawCX);
  const blob = new Blob([content], {type: 'application/octet-stream'});

  FD.append('CXNetworkStream', blob);

  XHR.addEventListener('load', evt => {
    const resCode = evt.target.status
    if(resCode !== 200) {
      // Failed to load.
      saveFailed(evt)
      return
    }

    const end = new Date().getTime();
    const time = end - start;
    console.log('## Upload time: ' + time);

    const newNdexId = evt.target.response;
    saveSuccess(newNdexId);
  });

  XHR.addEventListener('error', evt => {
    saveFailed(evt);
  });

  if(isUpdate) {
    XHR.open('PUT', url);
  } else {
    XHR.open('POST', url);
  }

  const auth = 'Basic ' + btoa(id + ':' + pass);
  XHR.setRequestHeader('Authorization', auth);
  start = new Date().getTime();
  console.log('## Upload Start');
  XHR.send(FD);
}

function saveSuccess(ndexIdStr) {

  const flagPrivate = 'PRIVATE';
  const flagPublic = 'PUBLIC';

  let visibility = flagPrivate

  if (isPrivate) {
    visibility = flagPrivate
  } else {
    visibility = flagPublic
  }

  const ndexId = ndexIdStr.replace(/"/g, '');

  const updateUrl = options.serverAddress + '/rest/network/' + ndexId + '/summary';
  const param = {
    method: 'post',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      Authorization: 'Basic ' + btoa(options.userName + ':' + options.userPass)
    },
    body: JSON.stringify({visibility: visibility})
  };


  console.log('URL for GET summary:')
  console.log(updateUrl)

  fetch(updateUrl, param)
    .then(response => {
      if (response.ok) {
        // Assign UUID
        assignNdexId(ndexId)

        // Save the image:
        getImage(options.SUID, ndexId);

        // win.close()
      } else {
        saveFailed(response);
      }
    });
}

function saveFailed(evt) {
  const errorMsg = MSG_ERROR
  errorMsg.detail = evt.target.response
  child.close()

  dialog.showMessageBox(errorMsg, () => {
    win.close()
  });
}

function assignNdexId(uuid) {
  // This should be assigned to the collection, not an individual network.

  const data = {
    key: 'SUID',
    dataKey: 'SUID',
    data: [
      {
        SUID: parseInt(options.rootSUID, 10),
        "ndex:uuid": uuid
      }
    ]
  }

  const params = {
    method: 'put',
    headers: HEADERS,
    body: JSON.stringify(data)
  }

  const url = 'http://localhost:' + options.cyrestPort +'/v1/collections/' + options.rootSUID + '/tables/default';
  fetch(url, params);
}

function postCollection() {
  const cxUrl = 'http://localhost:' + options.cyrestPort +'/v1/collections/' + options.rootSUID;
  fetch(cxUrl, {
    method: 'get',
    headers: HEADERS,
  }).then(response=> {
    return response.json();
  }).then(cx => {
    postCx(cx);
  });
}

function showLoading() {
  const gl = remote.getGlobal('sharedObj');
  const contentDir = gl.dir;
  child.loadURL('file://' + contentDir + '/webapp/ndex-save/waiting/index.html');
  child.once('ready-to-show', () => {
    child.show();
  });
}


function getImage(suid, uuid) {
  const url = 'http://localhost:' + options.cyrestPort +'/v1/networks/' + suid + '/views/first.png?h=2000';

  const fileName = path.join(tempDir, uuid + '.png')

  request.head(url, (err, res, body) => {
    console.log('content-type:', res.headers['content-type']);
    console.log('content-length:', res.headers['content-length']);

    request(url).pipe(fs.createWriteStream(fileName)).on('close', () => {

      minioClient.fPutObject('images', uuid + '.png', fileName, 'application/octet-stream', function(err, etag) {
        if(err !== null) {
          // Upload error
          dialog.showMessageBox(MSG_ERROR_IMAGE_SAVE, () => {
            child.close()
            win.close()
          })
        } else {
          console.log('Success!!!!!!!!')
          child.close()
          win.close()
        }
      })
    });
  });
}

// Start the application whenever the required parameters are ready.
ipcRenderer.on('ping', (event, arg) => {
  console.log(arg);

  console.log(event);

  if(arg === null || arg === undefined) {
    dialog.showMessageBox(win, MSG_ERROR_LOGIN, () => {
      win.close()
    });
  } else {
    options = arg;
    console.log('Options available:');
    console.log(options);
    startApp();
  }
})

function checkUuid(uuid) {
  const url = options.serverAddress + '/rest/network/' + uuid;

  const param = {
    method: 'get',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      Authorization: 'Basic ' + btoa(options.userName + ':' + options.userPass)
    }
  };

  fetch(url, param)
      .then(response => {
        if(!response.ok) {
          dialog.showMessageBox(win, MSG_ERROR_NOT_FOUND, () => {
            existingUuid = null
          })

          return null;
        } else {
          console.log('UUID found: ' + uuid)
          // Check permission
          return response.json()
        }
      })
    .then(json => {
      if(json !== null) {
        checkPermission(json)
      }
    })
}

function checkPermission(metadata) {

  console.log("Checking permission: ")
  console.log(metadata);

  const owner = metadata.owner.toLowerCase();
  if(owner !== options.userName.toLowerCase()) {
    dialog.showMessageBox(win, MSG_ERROR_NOT_OWNER, () => {
      existingUuid = null
    })
  }
}
