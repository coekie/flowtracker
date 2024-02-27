import type { Tracker } from '../javatypes'

export interface SelectedRange {
  tracker: Tracker
  offset: number
  length: number
}