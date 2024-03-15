import type { Tracker } from '../javatypes'

export interface SelectedRange {
  type: "range"
  tracker: Tracker
  offset: number
  length: number
}

export interface SelectedPath {
  type: "path"
  path: String[]
}

export type Selected = SelectedRange | SelectedPath

/** Returns which part (index) of `path` should be rendered as being selected */
export function indexInPath(selection: Selected | null, path: String[]|null):number|null {
  if (!selection || !path || selection.type !== "path" || !pathStartsWith(path, selection.path)) return null
  return selection.path.length - 1
}

/** Checks if array a starts with b */
export function pathStartsWith(a:String[], b:String[]):boolean {
  return a && b && a.length >= b.length && b.every((n, i) => a[i] == n)
}