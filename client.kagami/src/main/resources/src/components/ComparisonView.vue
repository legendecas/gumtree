<template>
  <div class="comparison-view">
    <div class="legend-view">
      <div class="legend-container">
        <div
          v-for="lgd in ['insert', 'update', 'move', 'delete']"
          :key="lgd"
          :class="['legend', lgd]"
        ><p>{{lgd}}</p></div>
      </div>
    </div>
    <div class="view src-view">
      <button @click="toggleView('src')">Toggle</button>
      <tree-view
        v-show="srcView === 'tree'"
        :data="srcTree"
        :options="{ maxDepth: 3, type: 'src' }"
      ></tree-view>
      <source-view
        v-show="srcView === 'source'"
        :source="srcSource"
        :actions="srcActions"
        :tree="srcTree"
      ></source-view>
    </div>
    <div class="view dest-view">
      <button @click="toggleView('dest')">Toggle</button>
      <tree-view
        v-show="destView === 'tree'"
        :data="destTree"
        :type="'dest'"
        :options="{ maxDepth: 3, type: 'dest' }"
      ></tree-view>
      <source-view
        v-show="destView === 'source'"
        :source="destSource"
        :actions="destActions"
        :tree="destTree"
      ></source-view>
    </div>
  </div>
</template>

<script>
import _ from 'lodash'
import bus from '../eventBus'

const resources = {
  src: {
    source: '/api/src/source',
    tree: '/api/src/tree'
  },
  dest: {
    source: '/api/dest/source',
    tree: '/api/dest/tree'
  },
  match: '/api/match'
}
if (process.env.NODE_ENV !== 'production') {
  resources.src = {
    source: '/static/source.py',
    tree: '/static/source.py.json'
  }
  resources.dest = {
    source: '/static/target.py',
    tree: '/static/target.py.json'
  }
  resources.match = '/static/diff.json'
}

export default {
  name: 'ComparisonView',
  data () {
    return {
      srcView: 'tree',
      srcTree: '{}',
      srcSource: '',
      destView: 'tree',
      destTree: '{}',
      destSource: '',
      match: {
        matches: [],
        actions: []
      }
    }
  },
  computed: {
    srcActions () {
      return _.map(this.match.actions, it => {
        let node
        switch (it.action) {
          case 'update':
          case 'delete':
          case 'move':
            node = it.tree
            break
        }
        return {
          action: it.action,
          node
        }
      }).filter(it => !_.isNil(it.node))
    },
    destActions () {
      return _.map(this.match.actions, it => {
        let node
        switch (it.action) {
          case 'insert':
            node = it.tree
            break
          case 'update':
          case 'move':
            node = this.findMatchIdx(it.tree)
            break
        }
        return {
          action: it.action,
          node
        }
      }).filter(it => !_.isNil(it.node))
    }
  },
  methods: {
    toggleView (type) {
      this[type + 'View'] = this[type + 'View'] === 'tree' ? 'source' : 'tree'
    },
    tagTree (node, idx = 0) {
      if (node.children) {
        _.forEach(node.children, child => {
          idx = this.tagTree(child, idx)
        })
      }
      if (!_.isNil(node.pos) && !_.isNil(node.length)) {
        node.start = Number(node.pos)
        node.end = Number(node.pos) + Number(node.length)
      }
      node.__idx = idx
      return idx + 1
    },
    tagSourceActions (tree, actions) {
      _.forEach(actions, it => {
        const node = this.findNode(it.tree, tree)
        if (_.isNil(node)) {
          return
        }
        switch (it.action) {
          case 'update':
          case 'delete':
          case 'move':
            node.__action = it.action
            break
        }
      })
    },
    tagTargetActions (tree, actions) {
      _.forEach(actions, it => {
        let node
        switch (it.action) {
          case 'insert':
            node = this.findNode(it.tree, tree)
            break
          case 'update':
            node = this.findNode(this.findMatchIdx(it.tree), tree)
            break
          case 'move':
            node = this.findNode(this.findMatchIdx(it.tree), tree)
            break
        }
        if (node) {
          node.__action = it.action
        }
      })
    },
    findNode (idx, tree) {
      if (tree.__idx === idx) {
        return tree
      }
      if (tree.children) {
        for (const it of tree.children) {
          const result = this.findNode(idx, it)
          if (result) {
            return result
          }
        }
      }
      return null
    },
    findMatchIdx (idx, type = 'src') {
      let found
      switch (type) {
        case 'src':
          found = _.find(this.match.matches, it => it.src === idx)
          return _.get(found, 'dest')
        case 'dest':
          found = _.find(this.match.matches, it => it.dest === idx)
          return _.get(found, 'src')
      }
    },
    onToggleOpen (event) {
      const { source, open, node, parent, key } = event
      const target = source === 'src' ? 'dest' : 'src'
      if (!_.isNil(node)) {
        const found = _.get(_.find(this.match.matches, it => it[source] === node), target)
        bus.$emit('toggleOpen', { target, open, node: found })
      } else if (!_.isNil(parent) && !_.isNil(key)) {
        const found = _.get(_.find(this.match.matches, it => it[source] === parent), target)
        bus.$emit('toggleOpen', { target, open, parent: found, key })
      }
    },
    async fetchJson (url) {
      const response = await fetch(url)
      if (response.status !== 200) {
        throw new Error('fetch source failed')
      }
      let sourceTree = await response.json()
      if (sourceTree.root && _.isNil(sourceTree.children)) {
        sourceTree = sourceTree.root
      }
      console.log('original', sourceTree)
      this.tagTree(sourceTree)
      return sourceTree
    },
    async fetchSource (url) {
      const response = await fetch(url)
      if (response.status !== 200) {
        throw new Error('fetch source failed')
      }
      const source = await response.text()
      return source
    },
    async fetchMatch (url) {
      const response = await fetch(url)
      if (response.status !== 200) {
        throw new Error('fetch match failed')
      }
      const match = await response.json()
      return match
    }
  },
  async created () {
    const match = await this.fetchMatch(resources.match)
    console.log(_.cloneDeep(match))
    this.match = match
    const [srcTree, destTree] = await Promise.all([this.fetchJson(resources.src.tree), this.fetchJson(resources.dest.tree)])
    console.log('src', _.cloneDeep(srcTree), 'dest', _.cloneDeep(destTree))
    this.srcTree = srcTree
    this.destTree = destTree
    this.tagSourceActions(this.srcTree, this.match.actions)
    this.tagTargetActions(this.destTree, this.match.actions)

    const [srcSource, destSource] = await Promise.all([this.fetchSource(resources.src.source), this.fetchSource(resources.dest.source)])
    this.srcSource = srcSource
    this.destSource = destSource

    bus.$on('onToggleOpen', e => this.onToggleOpen(e))
  },
  destroy () {

  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h1, h2 {
  font-weight: normal;
}

ul {
  list-style-type: none;
  padding: 0;
}

li {
  display: inline-block;
  margin: 0 10px;
}

a {
  color: #42b983;
}

.comparison-view {
  display: flex;
  flex-wrap: wrap;
}

.view {
  text-align: left;
  flex: 1;
}

.legend-view {
  padding-right: 8px;
  width: 100%;
}

.legend-container {
  display: flex;
  flex-direction: row-reverse;
  height: 2em;
}

.legend-view .legend {
  padding: -2 2px;
  margin: 2px;
  width: 4em;
}

.legend p {
  margin: 2px 0;
}

.legend-view .insert {
  background-color: #26A69A;
}

.legend-view .update {
  background-color: #FFCA28;
}

.legend-view .delete {
  background-color: #FF7043;
}

.legend-view .move {
  background-color: #29B6F6;
}
</style>
