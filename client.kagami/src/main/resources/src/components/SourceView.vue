<template>
  <div>
    <div>
      <pre><code class="python" v-html="rawSource"></code></pre>
    </div>
  </div>
</template>

<script>
import _ from 'lodash'
import StringReplacer from '@/StringReplacer'

export default {
  name: 'source-view',
  props: ['source', 'actions', 'tree'],
  computed: {
    rawSource () {
      if (this.source && this.tree && this.actions) {
        const source = this.tagSource(this.source, this.actions, this.tree)
        return source
      }
      return this.source
    }
  },
  methods: {
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
    tagSource (source, actions, tree) {
      const replacer = new StringReplacer(source)
      _.chain(actions)
        .map(it => {
          const { action, node: idx } = it
          const node = this.findNode(idx, tree)
          return { action, ..._.pick(node, 'start', 'end') }
        })
        .sortBy('start', 'end')
        .forEach(it => {
          const { action, start, end } = it
          const length = end - start
          replacer.replace(start, length, `<span class="${action}">`, '</span>')
        })
        .value()
      return replacer.toString()
    }
  }
}
</script>

<style>

pre code span.insert {
  background-color: #26A69A;
}

pre code span.update {
  background-color: #FFCA28;
}

pre code span.delete {
  background-color: #FF7043;
}

pre code span.move {
  background-color: #29B6F6;
}

</style>
