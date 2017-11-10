import Vue from 'vue'
import Router from 'vue-router'
import ComparisonView from '@/components/ComparisonView'
import SourceView from '@/components/SourceView'
import TreeView from '@/components/TreeView'
import TreeViewItem from '@/components/TreeViewItem'
import TreeViewItemValue from '@/components/TreeViewItemValue'

Vue.use(Router)
Vue.component('source-view', SourceView)
Vue.component('tree-view-item-value', TreeViewItemValue)
Vue.component('tree-view-item', TreeViewItem)
Vue.component('tree-view', TreeView)

export default new Router({
  routes: [
    {
      path: '/',
      name: 'ComparisonView',
      component: ComparisonView
    }
  ]
})
