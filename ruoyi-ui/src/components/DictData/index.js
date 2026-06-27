import { useDictStore } from '@/store/modules/dict'
import DataDict from '@/utils/dict'
import { getDicts } from '@/api/system/dict/data'

function install(app) {
  app.config.globalProperties.$dict = DataDict
  app.provide('dict', DataDict)

  // 预加载 * 类型字典
  const dictStore = useDictStore()
  getDicts('*').then(res => {
    dictStore.setDict(res.data)
  })
}

export default { install }
