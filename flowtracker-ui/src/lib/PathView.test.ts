import {render, screen} from '@testing-library/svelte'
import userEvent from '@testing-library/user-event'
import {expect, test} from 'vitest'

import PathView from './PathView.svelte'
import { Coloring } from './coloring'
import { PathSelection } from './selection'

const user = userEvent.setup()

test('simple path', () => {
  render(PathView, {path: ['node1', 'node2'], selection: null, coloring: new Coloring()})

  const node1 = screen.getByText('node1')
  expect(node1).toBeInTheDocument()
})

test('select path and render as selected', async () => {
  const pathView:PathView = render(PathView, {path: ['node1', 'node2', 'node3'], selection: null, coloring: new Coloring()}).component

  const node1 = screen.getByText('node1')
  const node2 = screen.getByText('node2')
  const node3 = screen.getByText('node3')
  expect(node1).not.toHaveClass('selected')
  expect(node2).not.toHaveClass('selected')
  expect(node3).not.toHaveClass('selected')

  await user.click(node2)

  expect(pathView.selection).toMatchObject({path: ['node1', 'node2']})
  expect(node1).not.toHaveClass('selected')
  expect(node2).toHaveClass('selected')
  expect(node3).not.toHaveClass('selected')
})

test('coloring', async () => {
  const coloring = new Coloring();
  coloring.add(new PathSelection(['node1', 'node2']))
  render(PathView, {path: ['node1', 'node2', 'node3'], selection: null, coloring: coloring}).component

  const node1 = screen.getByText('node1')
  const node2 = screen.getByText('node2')
  const node3 = screen.getByText('node3')
  expect(node1).toHaveStyle('background-color: inherit')
  expect(node2).toHaveStyle('background-color: ' + coloring.assignments[0].color)
  expect(node3).toHaveStyle('background-color: inherit')
})