const WS_SERVER = 'ws://localhost:8025/ws/echo';

const MESSAGES = {
  CONNECT: {
    from: 'ndex',
    type: 'connected',
    body: ''
  },

  ALIVE: {
    from: 'ndex',
    type: 'alive',
    body: 'from renderer'
  }
};

const EMPTY_NET = {
  data: {},
  elements: {
    nodes: [],
    edges: []
  }
};

module.exports = { WS_SERVER, MESSAGES, EMPTY_NET };
