const remote = require('electron').remote;
const Immutable = require('immutable');
const {ipcRenderer} = require('electron');
const jsonfile = require('jsonfile');

const config = require('./config_browser');
const fileUrl = require('file-url');

const {Menu, MenuItem} = require('electron').remote;

const CLOSE_BUTTON_ID = 'close';

const HEADERS = {
  Accept: 'application/json',
  'Content-Type': 'application/json'
};

const DEF_PUBLIC_NAME = 'NDEx Public Server';
const DEF_PUBLIC_SERVER = 'http://public.ndexbio.org';

const CYREST = {
  IMPORT_NET: 'http://localhost:1234/v1/networks?format=cx&source=url',
  COLLECTIONS: 'http://localhost:1234/v1/collections'
};

// Context Menu
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


let defaultState = Immutable.Map({
  serverName: DEF_PUBLIC_NAME,
  serverAddress: DEF_PUBLIC_SERVER,
  userName: "",
  userPass: "",
  loggedIn: false
});

let tempDir;
let cyrestPortNumber


// For saving to local directory
function storeFile(cx, fileName) {
  const file = tempDir + fileName;

  console.log('Temp File location: ' + file);
  return jsonfile.writeFileSync(file, cx);
}

// Get options from main process
ipcRenderer.on('ping', (event, arg) => {
  console.log(arg);
  console.log(event);
  loginInfo = arg;
  console.log('Login Options available:');
  console.log(loginInfo);

  const gl = remote.getGlobal('sharedObj');
  tempDir = gl.temp;

  if (loginInfo === undefined || loginInfo === null || loginInfo === {}) {
  } else {
    loginInfo['loggedIn'] = true;
    defaultState = Immutable.Map(loginInfo);
  }

  startApp();
});

// Name of the redux store
const STORE_NDEX = 'ndex';


const MESSAGE_TYPE = {
  QUERY: 'query'
};


let cyto;

function startApp() {
  addCloseButton();
}

function addCloseButton() {
  document.getElementById(CLOSE_BUTTON_ID)
    .addEventListener('click', () => {
      remote.getCurrentWindow().close();
    });

  init();
}

let cySocket;


/**
 * Create list of URLs for calling POST /v1/networks?source=url API
 *
 * @param idList
 * @param isPublic
 *
 * @returns {*}
 */
function createNetworkList(idList, isPublic) {
  const server = cyto.getStore(STORE_NDEX).server.toJS();
  return idList.map(id => {
    if (isPublic) {
      return server.serverAddress + '/rest/network/' + id + '/asCX';
    } else {

      const tmpFileUrl = fileUrl(tempDir + id + '.json')
      console.log("%% Got url: " + tmpFileUrl);

      return tmpFileUrl
    }
  })
}


function getImportQuery(ids, isPublic) {
  return {
    method: 'post',
    headers: HEADERS,
    body: JSON.stringify(createNetworkList(ids, isPublic))
  };
}

function applyLayout(results) {
  results.map(result => {
    const suid = result.networkSUID;
    console.log("LAYOUT: " + suid);
    console.log(typeof suid);
    fetch('http://localhost:1234/v1/apply/layouts/force-directed/' + suid);
  });
}


function getNetworkSummary(id) {
  const credentials = defaultState.toJS();
  const url = credentials.serverAddress + '/rest/network/' + id.externalId;

  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
  };

  if (credentials.userName !== undefined
    && credentials.userName !== null && credentials.userName !== '') {
    headers.Authorization = 'Basic ' + btoa(credentials.userName + ':' + credentials.userPass);
  }

  const param = {
    method: 'get',
    headers: headers
  };

  console.log("# fetch called: " + id.externalId);
  return fetch(url, param);
}


function getSummaries(ids) {
  return Promise.all(ids.map(getNetworkSummary));
}


/**
 *
 * Import networks as individual collections
 *
 * @param ids
 */
function importCollections(ids, toSingleCollection) {
  const privateNetworks = new Set();
  const idList = Object.keys(ids).map(key => {return ids[key]})

  let collectionName = null;

  getSummaries(idList)
    .then(responses => {
      return Promise.all(responses.map(rsp => {
        return rsp.json();
      }));
    })
    .then(res => {
      res.map(net => {
        console.log('__SUMMARY:')
        console.log(net);
        if(collectionName === null) {
          collectionName = net.name
        }

        // Check private or not
        if (net.visibility === 'PRIVATE') {
          privateNetworks.add(net.externalId);
        }
      });
    })
    .then(() => {
      // Download all private networks into temp dir.
      return Promise.all(idList.map(id => {
        if (privateNetworks.has(id.externalId)) {
          return fetchNetwork(id.externalId);
        }
      }))
    })
    .then(() => {
      // Import both private and public networks at once.
        console.log("-------- Download finished2.  Next import...")
      importAll(toSingleCollection, collectionName, idList, privateNetworks, true);
    });
}


/**
 * Download private networks into files
 *
 * @param uuid
 */
function fetchNetwork(uuid) {
  const credentials = defaultState.toJS();
  const address = credentials.serverAddress;
  const url = address + '/rest/network/' + uuid + '/asCX';

  const param = {
    method: 'get',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      Authorization: 'Basic ' + btoa(credentials.userName + ':' + credentials.userPass)
    }
  };

  return fetch(url, param)
    .then(response => {
      return response.json()
    })
    .then(json => {
      return storeFile(json, uuid + '.json');
    });
}


function assignNdexId(suid, uuid) {

  const param = {
    method: 'get',
    headers: HEADERS
  }

  const paramPut = {
    method: 'put',
    headers: HEADERS
  }

  // API to get SUID of the root network
  const baseUrl = CYREST.COLLECTIONS.replace('1234', cyrestPortNumber)
  const url = baseUrl + '?subsuid=' + suid

  fetch(url, param)
    .then(response => {
      return response.json();
    })
    .then(rootIdArray => {
      const rootId = rootIdArray[0]
      const urlPost = baseUrl + '/' + rootId + '/tables/default'
      paramPut.body = JSON.stringify(createAssignIdData(rootId, uuid))

      fetch(urlPost, paramPut)
    })
}


/**
 * If networks are imported into one collection, old UUID
 * Should be removed.
 * This function removes it.
 *
 * @param suid
 */
function deleteNdexId(suid) {
  const baseUrl = CYREST.COLLECTIONS.replace('1234', cyrestPortNumber)
  const url = baseUrl + '?subsuid=' + suid

  fetch(url)
      .then(response => {
        return response.json();
      })
      .then(rootIdArray => {
        const rootId = rootIdArray[0]
        const urlOriginal = baseUrl
            + '/' + rootId + '/tables/default/columns/ndex:uuid'

        console.log(urlOriginal)
        const urlDelete = encodeURIComponent(urlOriginal)
        console.log(urlDelete)

        const param = {
          method: 'delete',
          headers: HEADERS
        }

        fetch(urlOriginal, param)
      })
}


function createAssignIdData(suid, uuid) {
  return {
    key: 'SUID',
    dataKey: 'SUID',
    data: [
      {
        SUID: parseInt(suid, 10),
        'ndex:uuid': uuid
      }
    ]
  }
}


function setUUIDs(results) {
  results.map(entry => {
    // Array of SUIDs in the collection
    const networkIdList = entry.networkSUID
    const source = entry.source
    const parts = source.split('/')

    let uuid = null
    if(parts[parts.length - 1] === 'asCX') {
      uuid = parts[parts.length - 2]
    } else {
      uuid = parts[parts.length - 1].split('.')[0]
    }
    console.log('Assigning NDEx ID: ' + uuid)

    assignNdexId(networkIdList[0], uuid)
  })

}


function importAll(toSingleCollection, collectionName, ids, privateNetworks, doLayout) {
  const publicNets = [];
  const privateNets = [];

  ids.map(idObj => {
    const id = idObj.externalId

    if (privateNetworks.has(id)) {
      privateNets.push(id);
    } else {
      publicNets.push(id);
    }
  });

  let url = CYREST.IMPORT_NET.replace('1234', cyrestPortNumber)
  if(toSingleCollection) {
    url = url + '&collection=' + collectionName
  }

  // Public collections
  const q = getImportQuery(publicNets, true)
  const qPrivate = getImportQuery(privateNets, false)

  console.log(q)
  console.log(qPrivate)

  fetch(url, q)
    .then(response => {
      return response.json();
    })
    .then(json => {
      if(toSingleCollection && json.length != 0) {
        console.log("NEED to DELETE public ID#############")
        console.log(json)
        deleteNdexId(json[0]['networkSUID'][0])
      } else {
        setUUIDs(json);
      }
    })
    .then(() => {
      // Private collections
      fetch(url, getImportQuery(privateNets, false))
        .then(response => {
          return response.json();
        })
        .then(json => {
          if(toSingleCollection && json.length != 0) {
            console.log("NEED to DELETE#############")
            console.log(json)
            deleteNdexId(json[0]['networkSUID'][0])
          } else {
            setUUIDs(json);
          }
        });
    })

}



function init() {
  initCyComponent(defaultState);
  updateWindowProps(cyto.getStore(STORE_NDEX).server.toJS());
  initWsConnection();
}

function initWsConnection() {
  cySocket = new WebSocket(config.WS_SERVER);

  cySocket.onopen = () => {
    cySocket.send(JSON.stringify(config.MESSAGES.CONNECT));
  };

  cySocket.onmessage = event => {
    const msg = JSON.parse(event.data);
    if (msg.from !== 'cy3') {
      return;
    }

    switch (msg.type) {
      case MESSAGE_TYPE.QUERY:
      {
        const query = msg.body;
        const port = msg.options;
        console.log('+++++++++++++++ PORT')
        console.log(port)
        cyrestPortNumber = port


        cyto.render(NDExValetFinder, document.getElementById('valet'), {
          theme: {
            palette: {
              primary1Color: '#6E93B6',
              primary2Color: '#244060',
              primary3Color: '##EDEDED',
              accent1Color: '#D69121',
              accent2Color: '#E4E4E4',
              accent3Color: '##9695A6'
            }
          },
          style: {
            backgroundColor: '#EDEDED'
          },
          filters: [NDExPlugins.Filters.TextBox],
          visualizations: [
            NDExPlugins.NetworkViz.NetworkTable,
            NDExPlugins.NetworkViz.CardSmall,
            NDExPlugins.NetworkViz.CardLarge
          ],
          defaultQuery: query,
          onLoad: (ids, toSingleCollection) => {
            importCollections(ids, toSingleCollection)
          }
        });
        break;
      }
      default:
      {
        break;
      }
    }
  };

  // Keep alive by sending notification...
  setInterval(function () {
    cySocket.send(JSON.stringify(config.MESSAGES.ALIVE));
  }, 120000);

}


function initCyComponent(serverState) {

  // Just create new data store.
  cyto = CyFramework.config([NDExStore], {
    ndex: {
      server: serverState
    }
  })
}


function updateWindowProps(server) {
  remote.getCurrentWindow()
    .setTitle('Connected: ' + server.serverName + ' ( ' + server.serverAddress + ' )');
}

