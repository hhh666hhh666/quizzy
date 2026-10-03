import request, { unwrap } from './request'

// 分类没有对应的 VO 类型文件（quizzy-web 里用的是 any[]）；这里就地声明，避免改动 types 镜像。
export interface CategoryVO {
  id: number
  name: string
  sort: number
  createTime: string
}

export function listCategories() {
  return unwrap<CategoryVO[]>(request.get('/categories'))
}
