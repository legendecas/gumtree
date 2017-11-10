<template>
  <div class="tree-view-item">
    <div v-if="isObject(data)" class="tree-view-item-leaf" :class="[data.action]">
      <div class="tree-view-item-node" @click.stop="toggleOpen()" >
        <span :class="{opened: isOpen()}" class="tree-view-item-key tree-view-item-key-with-chevron">{{getKey(data)}}</span>
        <span class="tree-view-item-hint">({{data.idx}})</span>
        <span class="tree-view-item-hint" v-show="!isOpen() && data.children.length === 1">{{data.children.length}} property</span>
        <span class="tree-view-item-hint" v-show="!isOpen() && data.children.length !== 1">{{data.children.length}} properties</span>
      </div>
      <tree-view-item
        v-show="isOpen()"
        v-for="child in children"
        :key="getKey(child)"
        :max-depth="maxDepth"
        :current-depth="currentDepth + 1"
        :data="child"
        :parent="data.idx"
        :modifiable="modifiable"
        :type="type"
        @change-data="onChangeData"
      ></tree-view-item>
    </div>
    <div v-if="isArray(data)" class="tree-view-item-leaf">
      <div class="tree-view-item-node" @click.stop="toggleOpen()">
        <span :class="{opened: isOpen()}"  class="tree-view-item-key tree-view-item-key-with-chevron">{{getKey(data)}}</span>
        <span class="tree-view-item-hint" v-show="!isOpen() && data.children.length === 1">{{data.children.length}} item</span>
        <span class="tree-view-item-hint" v-show="!isOpen() && data.children.length !== 1">{{data.children.length}} items</span>
      </div>
      <tree-view-item
        v-show="isOpen()"
        v-for="child in children"
        :key="getKey(child)"
        :max-depth="maxDepth"
        :current-depth="currentDepth + 1"
        :data="child"
        :parent="data.idx"
        :modifiable="modifiable"
        :type="type"
        @change-data="onChangeData"
      ></tree-view-item>
    </div>
    <tree-view-item-value
      class="tree-view-item-leaf"
      v-if="isValue(data)"
      :key-string="getKey(data)"
      :data="data.value"
      :modifiable="modifiable"
      @change-data="onChangeData"
    ></tree-view-item-value>
  </div>
</template>

<script>
  import _ from 'lodash'
  import TreeViewItemValue from './TreeViewItemValue.vue'
  import bus from '../eventBus'

  export default {
    components: {
      TreeViewItemValue
    },
    name: 'tree-view-item',
    props: ['data', 'max-depth', 'current-depth', 'modifiable', 'parent', 'type'],
    data: function () {
      return {
        open: this.currentDepth < this.maxDepth
      }
    },
    computed: {
      children () {
        return _.filter(this.data.children, it => {
          if (_.isInteger(Number(it.key))) {
            return true
          }
          if (it.key === 'children') {
            return it.children.length > 0
          }
          return _.includes(['typeLabel', 'start', 'end', 'label'], it.key)
        })
      }
    },
    methods: {
      isOpen: function () {
        return this.open
      },
      toggleOpen: function () {
        this.open = !this.open
        bus.$emit('onToggleOpen', {
          source: this.type,
          open: this.open,
          node: this.data.idx,
          parent: this.parent,
          key: this.data.key
        })
      },
      isObject: function (value) {
        return value.type === 'object'
      },
      isArray: function (value) {
        return value.type === 'array'
      },
      isValue: function (value) {
        return value.type === 'value'
      },
      getKey: function (value) {
        if (_.isInteger(value.key)) {
          return value.key + ':'
        } else {
          return '"' + value.key + '":'
        }
      },
      isRootObject: function (value = this.data) {
        return value.isRoot
      },
      onChangeData: function (path, value) {
        path = _.concat(this.data.key, path)
        this.$emit('change-data', path, value)
      },
      contains (idx, data = this.data) {
        if (data.idx === idx) {
          return true
        }
        const children = _.find(data.children, it => it.key === 'children')

        return _.some(_.map(children, child => this.contains(idx, child)), Boolean)
      }
    },
    created () {
      bus.$on('toggleOpen', event => {
        const { target, open, node, parent, key } = event

        if (target === this.type) {
          if (open) {
            if (!_.isNil(node) && this.contains(node)) {
              this.open = open
            } else if (!_.isNil(parent) && !_.isNil(key)) {
              if (this.parent === parent && this.data.key === key) {
                this.open = open
              } else if (this.contains(parent)) {
                this.open = open
              }
            }
          } else {
            if ((!_.isNil(node) && this.data.idx === node) || (!_.isNil(parent) && this.parent === parent)) {
              this.open = open
            }
          }
        }
      })
    }
  }
</script>

<style scoped>
.tree-view-item {
  font-family: monaco, monospace;
  font-size: 14px;
  margin-left: 18px;
}
.tree-view-item-node {
  cursor: pointer;
  position: relative;
  white-space: nowrap;
}
.tree-view-item-leaf {
  white-space: nowrap;
}
.tree-view-item-key {
  font-weight: bold;
}
.tree-view-item-key-with-chevron {
  padding-left: 14px;
}
.tree-view-item-key-with-chevron.opened::before {
    top:4px;
    transform: rotate(90deg);
    -webkit-transform: rotate(90deg);
}
.tree-view-item-key-with-chevron::before {
    color: #444;
    content: '\25b6';
    font-size: 10px;
    left: 1px;
    position: absolute;
    top: 3px;
    transition: -webkit-transform .1s ease;
    transition: transform .1s ease;
    transition: transform .1s ease, -webkit-transform .1s ease;
    -webkit-transition: -webkit-transform .1s ease;
}
.tree-view-item-hint {
  color: #ccc
}

.tree-view-item-leaf .insert {
  background-color: #26A69A;
}

.tree-view-item-leaf .update {
  background-color: #FFCA28;
}

.tree-view-item-leaf .delete {
  background-color: #FF7043;
}

.tree-view-item-leaf .move {
  background-color: #29B6F6;
}
</style>
