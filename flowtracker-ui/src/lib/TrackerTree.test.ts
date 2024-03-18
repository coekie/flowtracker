import {render, screen, waitFor} from '@testing-library/svelte'
import userEvent, { type UserEvent } from '@testing-library/user-event'
import {describe, expect, test} from 'vitest'

import TrackerTree from './TrackerTree.svelte'
import { Coloring } from './coloring'

import { afterAll, afterEach, beforeAll } from "vitest";
import { server } from "../mocks/node";
import type { SelectedPath } from './selection'
 
beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const user:UserEvent = userEvent.setup()

// click to open a folder, and then select a tracker
test('navigate tree', async () => {
  const tree:TrackerTree = render(TrackerTree, {selectedTracker: null, selection: null, coloring: new Coloring()}).component

  const root = await screen.findByRole('button', {name: '<root>'})
  const category = await screen.findByRole('button', {name: 'Simple'})
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

test('coloring', async () => {
  const coloring:Coloring = new Coloring()
  coloring.add({type: 'path', path: ['Simple']})
  const tree:TrackerTree = render(TrackerTree, {selectedTracker: null, selection: null, coloring}).component

  const category = await screen.findByRole('button', {name: 'Simple'})

  expect(category).toHaveStyle({'background-color': coloring.assignments[0].color})
})

test('node with multiple names: selection of part of name', async () => {
  const tree:TrackerTree = render(TrackerTree, {selectedTracker: null, selection: null, coloring: new Coloring()}).component
  await user.click(await screen.findByRole('button', {name: 'CombinedPath'}))

  tree.selection = {type: 'path', path: ['CombinedPath', 'one', 'two']}
  expect((await screen.findByText("one"))).not.toHaveClass('selected')
  expect((await screen.findByText("two"))).toHaveClass('selected')
  expect((await screen.findByText("three"))).not.toHaveClass('selected')
})

test('node with multiple names: selection of full name', async () => {
  const tree:TrackerTree = render(TrackerTree, {selectedTracker: null, selection: null, coloring: new Coloring()}).component
  await user.click(await screen.findByRole('button', {name: 'CombinedPath'}))

  tree.selection = {type: 'path', path: ['CombinedPath', 'one', 'two', 'three']}
  expect((await screen.findByText("one"))).not.toHaveClass('selected')
  expect((await screen.findByText("two"))).not.toHaveClass('selected')
  expect((await screen.findByText("three"))).not.toHaveClass('selected')

  const button = (await screen.findByText("one")).parentElement
  expect(button).toHaveClass('selected')
})

test('node with multiple names: coloring of part of name', async () => {
  const coloring:Coloring = new Coloring()
  coloring.add({type: 'path', path: ['CombinedPath', 'one', 'two']})
  const tree:TrackerTree = render(TrackerTree, {selectedTracker: null, selection: null, coloring}).component
  await user.click(await screen.findByRole('button', {name: 'CombinedPath'}))

  expect(await screen.findByText("one")).toHaveStyle({'background-color': 'inherit'})
  expect(await screen.findByText("two")).toHaveStyle({'background-color': coloring.assignments[0].color})
  expect(await screen.findByText("three")).toHaveStyle({'background-color': 'inherit'})
})

test('node with multiple names: coloring of full name', async () => {
  const coloring:Coloring = new Coloring()
  coloring.add({type: 'path', path: ['CombinedPath', 'one', 'two', 'three']})
  const tree:TrackerTree = render(TrackerTree, {selectedTracker: null, selection: null, coloring}).component
  await user.click(await screen.findByRole('button', {name: 'CombinedPath'}))

  expect(await screen.findByText("one")).toHaveStyle({'background-color': 'inherit'})
  expect(await screen.findByText("two")).toHaveStyle({'background-color': 'inherit'})
  expect(await screen.findByText("three")).toHaveStyle({'background-color': 'inherit'})

  const button = (await screen.findByText("one")).parentElement
  expect(button).toHaveStyle({'background-color': coloring.assignments[0].color})
})
