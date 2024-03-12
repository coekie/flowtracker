import type { SelectedRange } from './selection'

const autoColors: string[] = [
  "#ffaaaa", "#aaffaa", "#aaaaff",
  "#ffffaa", "#ffaaff", "#aaffff",
  "#dd7777", "#77dd77", "#7777dd",
  "#dddd77", "#dd77dd", "#77dddd",
]

export class ColorAssignment {
  color: string
  ranges: SelectedRange[]

  constructor(color: string, ranges:SelectedRange[]) {
    this.color = color
    this.ranges = ranges
  }
}

export class Coloring {
  assignments:ColorAssignment[] = []

  add(range: SelectedRange | null):void {
    if (!range) return;
    let color = autoColors[Math.min(this.assignments.length, autoColors.length-1)]
    this.assignments.push(new ColorAssignment(color, [range]))
  }

  canAdd():boolean {
    return autoColors.length > this.assignments.length
  }
}