# CyNDEx-2
The Cytoscape client for NDEx database

## Introduction
(TBD)
An Electron-based hybrid App for Cytoscape.

## Build
(TBD)

This application consists of two parts:

1. Electron web application
1. Cytoscape app written in Java

```bash
mvn clean install
```

## Install
(TBD)

## New: CyREST API
This app adds new endpoints to Cytoscape and you can use them to programmatically access some of the features of this application.

### Endpoints

Root of this API is ```http://localhost:1234/ndex/v1```.

Port number depends on your _rest.port_ Cytoscape property setting.

#### GET /
Show status of the App


##### Sample response
```json
{
  "apiVersion": "v1",
  "appName": "CyNDEx-2",
  "appVersion": "2.0.0"
}
```

#### GET /networks/current
Returns summary of the current network.

##### Sample response
```json
{
  "currentNetworkSuid": 25230,
  "currentRootNetwork": {
    "suid": 36,
    "name": "MyCollection1",
    "props": {
      "name": "MyCollection1",
      "SUID": 36,
      "selected": false
    }
  },
  "members": [
    {
      "suid": 52,
      "name": "BIOGRID-ORGANISM-Caenorhabditis_elegans-3.4.129.mitab",
      "props": {
        "shared name": "BIOGRID-ORGANISM-Caenorhabditis_elegans-3.4.129.mitab",
        "__Annotations": [],
        "name": "BIOGRID-ORGANISM-Caenorhabditis_elegans-3.4.129.mitab",
        "SUID": 52,
        "selected": false
      }
    },
    {
      "suid": 25230,
      "name": "galFiltered.sif",
      "props": {
        "shared name": "galFiltered.sif",
        "__Annotations": [],
        "name": "galFiltered.sif",
        "SUID": 25230,
        "selected": true
      }
    }
  ]
}
```

#### POST /networks
Create new network from an NDEx entry.

##### Request body

```json
{
    "uuid": "0268f115-b021-11e6-831a-06603eb7f303",
    "serverUrl": "http://www.ndexbio.org/v2",
    "userId": "myid",
    "password": "mypassword"
}
```

* uuid - UUID of the NDEx network
* serverUrl - URL of the NDEx API server
* userId - (Optional) NDEx user ID for loading private network
* password - (Optional) NDEx password for loading private network

##### Sample response
```json
{
  "suid":52,
  "uuid":"b5cb599b-ae23-11e6-831a-06603eb7f303"
}
```

#### POST /networks/SUID
Upload a Cytoscape network to NDEx server specified by the caller.

##### Request body
```json
{
  "isPublic": false,
  "userId": "myid",
  "password": "mypw",
  "serverUrl": "http://www.ndexbio.org/v2",
  "metadata": {
    "ndex.description": "Sample description from Cytoscape",
    "name": "Network name updated via rest",
    "ndex.species": "human"
  }
}
```

##### Sample response
```json
{
  "suid":3320,
  "uuid":"4a23aef0-2ba5-11e7-8f50-0ac135e8bacf"
}
```

#### POST /networks/current
Utility function to upload current networks to NDEx.  It actually calls ```POST /networks/SUID``` where SUID is the current network's SUID.

(same as above)


#### PUT /networks/SUID
Update the existing NDEx entry.

(TBD)


## License
MIT
