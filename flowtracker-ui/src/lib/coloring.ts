import type { Selected } from './selection'

const autoColors: string[] = [
  "#ffaaaa", "#aaffaa", "#aaaaff",
  "#ffffaa", "#ffaaff", "#aaffff",
  "#dd7777", "#77dd77", "#7777dd",
  "#dddd77", "#dd77dd", "#77dddd",
]

export class ColorAssignment {
  color: string
  selections: Selected[]

  constructor(color: string, selections:Selected[]) {
    this.color = color
    this.selections = selections
  }
}

export class Coloring {
  //assignments:ColorAssignment[] = []
  // TODO temporary code for testing coloring of paths, until we can create them in the UI
  assignments:ColorAssignment[] = [new ColorAssignment("#ffaaaa", [{type:"path", path:["Files"]}])]

  add(selection: Selected | null):void {
    if (!selection) return;
    let color = autoColors[Math.min(this.assignments.length, autoColors.length-1)]
    this.assignments.push(new ColorAssignment(color, [selection]))
  }

  canAdd():boolean {
    return autoColors.length > this.assignments.length
  }
}