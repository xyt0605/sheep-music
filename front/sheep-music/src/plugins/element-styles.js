// Element Plus 按需引入后，模板里的 <el-xxx> 组件样式由 unplugin-vue-components
// 的 resolver 自动引入；但 ElMessage / ElMessageBox / ElNotification / v-loading
// 是通过 JS API/指令调用的，不走模板编译，样式需要在这里显式引入。
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/notification/style/css'
import 'element-plus/es/components/loading/style/css'
