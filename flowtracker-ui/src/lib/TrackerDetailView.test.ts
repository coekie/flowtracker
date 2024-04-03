import {render, screen} from '@testing-library/svelte';
import userEvent, {type UserEvent} from '@testing-library/user-event';
import {describe, expect, test} from 'vitest';

import TrackerDetailView from './TrackerDetailView.svelte';
import {Coloring} from './coloring';

import {afterAll, afterEach, beforeAll} from 'vitest';
import {server} from '../mocks/node';
import {simpleOriginTracker, simpleSinkTracker} from '../mocks/handlers';
import {PathSelection, RangeSelection} from './selection';

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const user: UserEvent = userEvent.setup();

function renderSimpleSinkTracker(): TrackerDetailView {
  return render(TrackerDetailView, {
    viewTracker: simpleSinkTracker,
    selection: null,
    coloring: new Coloring(),
  }).component;
}

function renderSimpleOriginTracker(): TrackerDetailView {
  return render(TrackerDetailView, {
    viewTracker: simpleOriginTracker,
    targetTracker: simpleSinkTracker,
    selection: null,
    coloring: new Coloring(),
  }).component;
}

describe('select region', () => {
  const testIt = async (tree: TrackerDetailView) => {
    const foo = await screen.findByText('foo');
    const bar = await screen.findByText('bar');

    expect(foo).not.toHaveClass('selected');
    expect(bar).not.toHaveClass('selected');
    await user.click(foo);
    expect(foo).toHaveClass('selected');
    expect(bar).not.toHaveClass('selected');
    expect(tree.selection).toMatchObject({
      tracker: simpleOriginTracker,
      offset: 10,
      length: 3,
    });
  };

  test('in sink tracker', async () => {
    const tree: TrackerDetailView = renderSimpleSinkTracker();
    await testIt(tree);
    // selection in sink tracker view is what we should show in the origin tracker view
    expect(tree.secondaryTracker).toMatchObject(simpleOriginTracker);
  });

  test('in origin tracker', async () => {
    await testIt(renderSimpleOriginTracker());
  });
});

describe('select multiple regions', () => {
  const testIt = async (tree: TrackerDetailView) => {
    const foo = await screen.findByText('foo');
    const bar = await screen.findByText('bar');

    await user.pointer([
      // mouse down on foo
      {keys: '[MouseLeft>]', target: foo},
      // move mouse to bar
      {target: bar},
      // mouse up
      {keys: '[/MouseLeft]'},
    ]);

    expect(foo).toHaveClass('selected');
    expect(bar).toHaveClass('selected');
    expect(tree.selection).toMatchObject({
      tracker: simpleOriginTracker,
      offset: 10,
      length: 13,
    });
  };

  test('in sink tracker', async () => {
    await testIt(renderSimpleSinkTracker());
    expect(await screen.findByText(',')).not.toHaveClass('selected');
  });

  test('in origin tracker', async () => {
    await testIt(renderSimpleOriginTracker());
    expect(await screen.findByText('Value2:')).toHaveClass('selected');
  });
});

describe('select multiple regions in reverse', () => {
  const testIt = async (tree: TrackerDetailView) => {
    const foo = await screen.findByText('foo');
    const bar = await screen.findByText('bar');

    await user.pointer([
      // mouse down on bar
      {keys: '[MouseLeft>]', target: bar},
      // move mouse to foo
      {target: foo},
      // mouse up
      {keys: '[/MouseLeft]'},
    ]);

    expect(foo).toHaveClass('selected');
    expect(bar).toHaveClass('selected');
    expect(tree.selection).toMatchObject({
      tracker: simpleOriginTracker,
      offset: 10,
      length: 13,
    });
  };

  test('in sink tracker', async () => {
    await testIt(renderSimpleSinkTracker());
    expect(await screen.findByText(',')).not.toHaveClass('selected');
  });

  test('in origin tracker', async () => {
    await testIt(renderSimpleOriginTracker());
    expect(await screen.findByText('Value2:')).toHaveClass('selected');
  });
});

describe('coloring', () => {
  const testIt = async (tree: TrackerDetailView) => {
    const coloring = new Coloring();
    coloring.add(new RangeSelection(simpleOriginTracker, 10, 3));
    coloring.add(new RangeSelection(simpleOriginTracker, 20, 3));
    tree.coloring = coloring;

    const foo = await screen.findByText('foo');
    const bar = await screen.findByText('bar');

    expect(foo).toHaveStyle({
      'background-color': coloring.assignments[0].color,
    });
    expect(bar).toHaveStyle({
      'background-color': coloring.assignments[1].color,
    });
  };

  test('in sink tracker', async () => {
    await testIt(renderSimpleSinkTracker());
  });

  test('in origin tracker', async () => {
    await testIt(renderSimpleOriginTracker());
  });
});

test('coloring uses most specific path', async () => {
  const tree = renderSimpleSinkTracker();
  const coloring = new Coloring();
  coloring.add(new PathSelection(['Simple']));
  coloring.add(new PathSelection(['Simple', 'origin1']));
  tree.coloring = coloring;

  const foo = await screen.findByText('foo');

  expect(foo).toHaveStyle({
    'background-color': coloring.assignments[1].color,
  });
});
