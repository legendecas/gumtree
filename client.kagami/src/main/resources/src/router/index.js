import Vue from 'vue'
import Router from 'vue-router'
import HelloWorld from '@/components/HelloWorld'
import TreeView from '@/components/TreeView'
import TreeViewItem from '@/components/TreeViewItem'
import TreeViewItemValue from '@/components/TreeViewItemValue'

Vue.use(Router)
Vue.component('tree-view-item-value', TreeViewItemValue)
Vue.component('tree-view-item', TreeViewItem)
Vue.component('tree-view', TreeView)

export default new Router({
  routes: [
    {
      path: '/',
      name: 'Hello',
      component: HelloWorld
    }
  ]
})
