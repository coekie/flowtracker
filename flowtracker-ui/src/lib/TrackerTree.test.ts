import {render, screen, waitFor} from '@testing-library/svelte'
import userEvent from '@testing-library/user-event'
import {expect, test} from 'vitest'

import TrackerTree from './TrackerTree.svelte'
import { Coloring } from './coloring'

import { afterAll, afterEach, beforeAll } from "vitest";
import { server } from "../mocks/node";
 
beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

// click to open a folder, and then select a tracker
test('navigate tree', async () => {
  const user = userEvent.setup()
  const tree = render(TrackerTree, {selectedTracker: null, selection: null, coloring: new Coloring()}).component

  const root = await screen.findByRole('button', {name: '<root>'})
  const category = await screen.findByRole('button', {name: 'Category'})
  expect(root).toHaveClass('expanded')
  expect(category).not.toHaveClass('expanded')

  // open the "Category" path
  await user.click(category)
  expect(category).toHaveClass('expanded')
  expect(category).toHaveClass('selected')
  const tracker1 = await screen.findByRole('button', {name: 'tracker1'})
  expect(tracker1).toHaveClass('tracker')

  // select a tracker
  expect(tree.selectedTracker).toBeNull()
  await user.click(tracker1)
  expect(tracker1).toHaveClass('selected')
  expect(tree.selectedTracker).toMatchObject({id: 101})
})

// TODO test coloring
// TODO test part of a node's names being selected/colored