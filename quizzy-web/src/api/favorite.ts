import request, { unwrap } from './request'
import type { FavoriteFolderVO, PageResult, QuestionListItemVO } from '@/types'

/**
 * 收藏夹。模型见 `docs/adr/0030`，三处最容易搞混的地方先写在这里：
 *
 * - **收藏 = 题目在至少一个夹里**，没有「未分组」；所以「取消收藏」是从**所有夹**移出；
 * - 「修改收藏夹」是**覆盖**语义（`setQuestionFolders`），**空数组 = 留在默认夹**，不是取消收藏；
 * - 批量「加入收藏夹」（`addQuestionsToFolder`）是**只加不减**——`setQuestionFolders` 才会移动题。
 */

export function listFolders() {
  return unwrap<FavoriteFolderVO[]>(request.get('/favorites/folders'))
}

/**
 * 新建 / 编辑收藏夹的请求体。
 *
 * ⚠️ `intro` / `isPublic` **不许做成可选发送**：编辑面板里三个字段永远一起提交，
 * 后端也按整体覆盖写回——不发某个字段等于把它清掉，这正是「能清空简介」的实现方式。
 */
export interface FavoriteFolderSavePayload {
  name: string
  intro?: string
  isPublic?: boolean
}

export function createFolder(name: string, intro?: string, isPublic?: boolean) {
  return unwrap<FavoriteFolderVO>(request.post('/favorites/folders', { name, intro, isPublic }))
}

/** 编辑。默认收藏夹也走这个接口——它不可删，但可以改名。 */
export function updateFolder(folderId: number, payload: FavoriteFolderSavePayload) {
  return unwrap<void>(request.put(`/favorites/folders/${folderId}`, payload))
}

/** 删夹只把题从它里面移出；题若因此不属于任何夹，就不再是收藏。默认夹删不了（后端会拒）。 */
export function deleteFolder(folderId: number) {
  return unwrap<void>(request.delete(`/favorites/folders/${folderId}`))
}

/**
 * 某个收藏夹（`folderId` 为空 = **全部收藏**）里的题，**按最近收藏的排最前**。
 *
 * ⚠️ 它**不是**题库列表接口：题库页筛收藏走的是 `QuestionQuery.anyFavorite` /
 * `favoriteFolderIds`，那是**题目视角**（按题目 id 倒序）。这里是**收藏视角**，
 * 排序维度只存在于关联表上，所以后端另开了一个端点。理由见 `docs/adr/0030`。
 */
export function pageFavoriteQuestions(folderId: number | null, page: number, size: number) {
  return unwrap<PageResult<QuestionListItemVO>>(
    request.get('/favorites/questions', {
      params: { folderId: folderId ?? undefined, page, size }
    })
  )
}

/** 批量加入某个夹（只加不减）。 */
export function addQuestionsToFolder(folderId: number, questionIds: number[]) {
  return unwrap<void>(request.post(`/favorites/folders/${folderId}/questions`, { questionIds }))
}

/** 把某道题从某个夹里移出。 */
export function removeFromFolder(folderId: number, questionId: number) {
  return unwrap<void>(request.delete(`/favorites/folders/${folderId}/questions/${questionId}`))
}

/** 收藏：落到默认收藏夹，返回它——界面据此提示「已加入『X』」。 */
export function favorite(questionId: number) {
  return unwrap<FavoriteFolderVO>(request.post(`/favorites/questions/${questionId}`))
}

/**
 * 取消收藏：从所有夹移出。
 *
 * @returns 被移出的夹 id —— 交给界面做一次「撤销」，**不需要新接口**（原样设回去即可）
 */
export function unfavorite(questionId: number) {
  return unwrap<number[]>(request.delete(`/favorites/questions/${questionId}`))
}

/** 这道题现在在哪些夹里（用于「修改收藏夹」面板与撤销）。 */
export function getQuestionFolders(questionId: number) {
  return unwrap<number[]>(request.get(`/favorites/questions/${questionId}/folders`))
}

/**
 * 覆盖式设置题目所属收藏夹。
 *
 * ⚠️ 传空数组 = **留在默认收藏夹**，而不是取消收藏（见 `docs/adr/0030`）。
 */
export function setQuestionFolders(questionId: number, folderIds: number[]) {
  return unwrap<void>(request.put(`/favorites/questions/${questionId}/folders`, { folderIds }))
}

/** 用收藏的题开一次练习；`folderId` 不给表示「全部收藏」。 */
export function practiceFavorites(folderId: number | null, count: number) {
  return unwrap<number>(
    request.post('/favorites/practice', null, { params: { folderId: folderId ?? undefined, count } })
  )
}
