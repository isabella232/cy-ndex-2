# CyNDEx-2 Electron App

## Introduction
This is an application to use [Electron](http://electron.atom.io/) as a new frontend for Cytoscape app.

**This Electron app is designed to work as a part of Cytoscape app,** and is not designed as a stand-alone application for browsing NDEx.

## Project Structure

### Files
- `package.json` - Points to the app's main file and lists its details and dependencies.
- `main.js` - Starts the app and creates a browser window to render HTML. This is the app's **main process**.
- `render.js` - JavaScript code for the page renderer.

## Run
You **cannot** test complete application only with this project, but you can test the some of the features, such as searching NDEx database.

```bash
npm install && npm start
```

## Build the Application


## License:
* Source Code: [MIT License](https://opensource.org/licenses/MIT)
* Documentation: [CC-BY 4.0](https://creativecommons.org/licenses/by/4.0/)

----
&copy; 2017 The Cytoscape Consortium

Keiichiro Ono (University of California, San Diego)
