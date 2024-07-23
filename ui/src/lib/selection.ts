import type {Tracker} from '../javatypes';

/** A range of characters in a tracker */
export class RangeSelection {
  tracker: Tracker;
  offset: number;
  length: number;

  constructor(tracker: Tracker, offset: number, length: number) {
    this.tracker = tracker;
    this.offset = offset;
    this.length = length;
  }

  eq(o: object): boolean {
    return (
      o instanceof RangeSelection &&
      this.tracker.id == o.tracker.id &&
      this.offset == o.offset &&
      this.length == o.length
    );
  }
}

/** A path, pointing to a tracker or sub-path/folder containing multiple trackers */
export class PathSelection {
  path: string[];

  constructor(path: string[]) {
    this.path = path;
  }

  eq(o: object): boolean {
    return (
      o instanceof PathSelection &&
      this.path.length == o.path.length &&
      this.path.every((n, i) => o.path[i] == n)
    );
  }
}

/**
 * Anything that can be selected. (Not calling this `Selection` to avoid naming conflict)
 */
export type ASelection = RangeSelection | PathSelection;

/** Returns which part (index) of `path` should be rendered as being selected */
export function indexInPath(
  selection: ASelection | null,
  path: string[] | null
): number | null {
  if (
    !path ||
    !(selection instanceof PathSelection) ||
    !pathStartsWith(path, selection.path)
  )
    return null;
  return selection.path.length - 1;
}

/** Checks if array a starts with b */
export function pathStartsWith(a: string[], b: string[]): boolean {
  return a && b && a.length >= b.length && b.every((n, i) => a[i] == n);
}

export type OnTrackerSelected = (_: Tracker) => void;

/** Check if any child with class 'selected' is in the view, if not then scroll it into view */
export function scrollSelectedIntoView(container: HTMLElement): void {
  const selected = container.querySelectorAll('.selected');
  if (selected.length == 0) {
    return; // nothing to scroll to
  }
  for (let i = 0; i < selected.length; i++) {
    const s = selected[i];
    // if one of the selected regions is visible, then we don't need to scroll
    if (
      s.getBoundingClientRect().top <
        container.getBoundingClientRect().bottom &&
      s.getBoundingClientRect().bottom > container.getBoundingClientRect().top
    ) {
      return;
    }
  }
  // don't try to scroll in test env where it's not supported
  if (typeof selected[0].scrollIntoView == 'function') {
    // scroll to the first one
    selected[0].scrollIntoView({block: 'center'});
  }
}
