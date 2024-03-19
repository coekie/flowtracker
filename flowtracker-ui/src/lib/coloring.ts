import { indexInPath, type ASelection } from './selection'

const autoColors: string[] = [
  "#ffaaaa", "#aaffaa", "#aaaaff",
  "#ffffaa", "#ffaaff", "#aaffff",
  "#dd7777", "#77dd77", "#7777dd",
  "#dddd77", "#dd77dd", "#77dddd",
]

/**
 * The assignment of a color to what (which `ASelection`) should be rendered in that color.
 */
export class ColorAssignment {
  color: string
  selections: ASelection[]

  constructor(color: string, selections:ASelection[]) {
    this.color = color
    this.selections = selections
  }
}

/**
 * Contains the full configuration of how colors are assigned.
 */
export class Coloring {
  assignments:ColorAssignment[] = []

  add(selection: ASelection | null):void {
    if (!selection) return;
    let color = autoColors[Math.min(this.assignments.length, autoColors.length-1)]
    this.assignments.push(new ColorAssignment(color, [selection]))
  }

  canAdd():boolean {
    return autoColors.length > this.assignments.length
  }

  /**
   * Determines how `path` should be rendered according to this Coloring:
   * which part of it should be colored.
   */
  calcColorByIndex(path: String[]|null):ColorByIndex {
    const result:ColorByIndex = {}
    for (var assignment of this.assignments) {
      for (var selection of assignment.selections) {
        let index:number|null = indexInPath(selection, path)
        if (index != null && !result[index]) {
          result[index] = assignment.color
        }
      }
    }
    return result
  }
}

/** Mapping from index of parts of a path to the color in which that part should be rendered */
export interface ColorByIndex {
  [key: number]: string;
}