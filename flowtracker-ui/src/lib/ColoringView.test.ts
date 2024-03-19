import {render, screen} from '@testing-library/svelte';
import userEvent from '@testing-library/user-event';
import {expect, test} from 'vitest';

import ColoringView from './ColoringView.svelte';
import {Coloring, autoColors} from './coloring';
import {PathSelection} from './selection';

const selection1: PathSelection = new PathSelection(['one']);
const selection2: PathSelection = new PathSelection(['two']);

const user = userEvent.setup();

function getSquares(): HTMLElement[] {
  return screen.getAllByRole('link');
}

function getSquare(index: number): HTMLElement {
  return getSquares()[index];
}

test('render colorings', () => {
  const coloring: Coloring = new Coloring();
  coloring.add(selection1);
  coloring.add(selection2);
  render(ColoringView, {coloring, selection: null});

  expect(getSquares()).toHaveLength(3);
  expect(getSquare(0)).toHaveStyle({'background-color': autoColors[0]});
  expect(getSquare(1)).toHaveStyle({'background-color': autoColors[1]});
  expect(getSquare(2)).toHaveTextContent('+');
});

test('add coloring', async () => {
  const coloring: Coloring = new Coloring();
  render(ColoringView, {coloring, selection: selection1});
  await user.click(screen.getByText('+'));
  expect(coloring.assignments).toHaveLength(1);
  expect(coloring.assignments[0]).toMatchObject({
    color: autoColors[0],
    selections: [selection1],
  });
});

test('add selection to coloring', async () => {
  const coloring: Coloring = new Coloring();
  coloring.add(selection1);
  render(ColoringView, {coloring, selection: selection2});
  await user.click(getSquare(0));
  expect(coloring.assignments[0]).toMatchObject({
    color: autoColors[0],
    selections: [selection1, selection2],
  });
});

test('remove selection from coloring', async () => {
  const coloring: Coloring = new Coloring();
  coloring.add(selection1);
  coloring.assignments[0].selections.push(selection2);
  render(ColoringView, {coloring, selection: selection1});
  await user.click(getSquare(0));
  expect(coloring.assignments[0]).toMatchObject({
    color: autoColors[0],
    selections: [selection2],
  });
});

test('remove last selection from coloring', async () => {
  const coloring: Coloring = new Coloring();
  coloring.add(selection1);
  render(ColoringView, {coloring, selection: selection1});
  await user.click(getSquare(0));
  expect(coloring.assignments).toHaveLength(0);
});

test('reuse unused autocolor', async () => {
  const coloring: Coloring = new Coloring();
  coloring.add(selection1);
  coloring.add(selection2);
  render(ColoringView, {coloring, selection: selection1});
  await user.click(getSquare(0));
  expect(coloring.assignments).toHaveLength(1);
  await user.click(screen.getByText('+'));
  expect(coloring.assignments).toMatchObject([
    {color: autoColors[1], selections: [selection2]},
    {color: autoColors[0], selections: [selection1]},
  ]);
});
