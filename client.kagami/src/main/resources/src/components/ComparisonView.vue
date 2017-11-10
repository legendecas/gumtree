<template>
  <div class="comparison-view">
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
      node.__idx = idx
      return idx + 1
    },
    tagSourceActions (tree, actions) {
      _.forEach(actions, it => {
        const node = this.findNode(it.tree, tree)
        switch (it.action) {
          case 'update':
            node.__action = 'update'
            break
          case 'delete':
            node.__action = 'delete'
            break
          case 'move':
            node.__action = 'move'
            break
        }
      })
    },
    tagTargetActions (tree, actions) {
      _.forEach(actions, it => {
        switch (it.action) {
          case 'insert':
            this.findNode(it.tree, tree).__action = 'insert'
            break
          case 'update':
            this.findNode(this.findMatchIdx(it.tree), tree).__action = 'update'
            break
          case 'move':
            this.findNode(this.findMatchIdx(it.tree), tree).__action = 'move'
            break
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
      const sourceTree = await response.json()
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
    const match = await this.fetchMatch('/static/diff.json')
    this.match = match
    const [srcTree, destTree] = await Promise.all([this.fetchJson('/static/source.py.json'), this.fetchJson('/static/target.py.json')])
    this.srcTree = srcTree
    this.destTree = destTree
    this.tagSourceActions(this.srcTree, this.match.actions)
    this.tagTargetActions(this.destTree, this.match.actions)

    const [srcSource, destSource] = await Promise.all([this.fetchSource('/static/source.py'), this.fetchSource('/static/target.py')])
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
}

.view {
  text-align: left;
  flex: 1;
}
</style>
