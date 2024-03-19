import {indexInPath, type ASelection} from './selection';

export const autoColors: string[] = [
  '#ffaaaa',
  '#aaffaa',
  '#aaaaff',
  '#ffffaa',
  '#ffaaff',
  '#aaffff',
  '#dd7777',
  '#77dd77',
  '#7777dd',
  '#dddd77',
  '#dd77dd',
  '#77dddd',
];

/**
 * The assignment of a color to what (which `ASelection`) should be rendered in that color.
 */
export class ColorAssignment {
  color: string;
  selections: ASelection[];

  constructor(color: string, selections: ASelection[]) {
    this.color = color;
    this.selections = selections;
  }

  remove(selection: ASelection): boolean {
    for (let i = 0; i < this.selections.length; i++) {
      if (selection.eq(this.selections[i])) {
        this.selections.splice(i, 1);
        return true;
      }
    }
    return false;
  }
}

/**
 * Contains the full configuration of how colors are assigned.
 */
export class Coloring {
  assignments: ColorAssignment[] = [];

  add(selection: ASelection | null): void {
    if (!selection) return;
    // first unused color from autoColors
    const color =
      autoColors.find(c => !this.assignments.some(a => a.color == c)) ??
      '#eeeeee';
    this.assignments.push(new ColorAssignment(color, [selection]));
  }

  canAdd(): boolean {
    return autoColors.length > this.assignments.length;
  }

  /**
   * Determines how `path` should be rendered according to this Coloring:
   * which part of it should be colored.
   */
  calcColorByIndex(path: string[] | null): ColorByIndex {
    const result: ColorByIndex = {};
    for (const assignment of this.assignments) {
      for (const selection of assignment.selections) {
        const index: number | null = indexInPath(selection, path);
        if (index != null && !result[index]) {
          result[index] = assignment.color;
        }
      }
    }
    return result;
  }
}

/** Mapping from index of parts of a path to the color in which that part should be rendered */
export interface ColorByIndex {
  [key: number]: string;
}
