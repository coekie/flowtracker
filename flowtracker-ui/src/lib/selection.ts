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