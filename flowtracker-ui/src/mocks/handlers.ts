import {http, HttpHandler, HttpResponse} from 'msw';
import type {Tracker} from '../javatypes';

export const simpleOriginTracker = {
  id: 101,
  path: ['Simple', 'origin1'],
  origin: true,
  sink: false,
};

export const simpleSinkTracker = {
  id: 102,
  path: ['Simple', 'sink1'],
  origin: false,
  sink: true,
};

export const classOriginTracker = {
  id: 103,
  path: ['Class', 'myClass'],
  origin: true,
  sink: false,
};

export const handlers: HttpHandler[] = [
  http.get('/tree/all', () => {
    return HttpResponse.json({
      names: ['<root>'],
      children: [
        {
          names: ['Simple'],
          children: [
            {
              names: ['origin1'],
              children: [],
              tracker: simpleOriginTracker,
            },
            {
              names: ['sink1'],
              children: [],
              tracker: simpleSinkTracker,
            },
          ],
        },
        // example where multiples parts of the path are squashed together in one node
        {
          names: ['CombinedPath'],
          children: [
            {
              names: ['one', 'two', 'three'],
              children: [
                {
                  names: ['tracker1'],
                  children: [],
                  tracker: {
                    id: 201,
                    path: ['CombinedPath', 'one', 'two', 'three', 'tracker1'],
                    origin: true,
                    sink: false,
                  },
                },
              ],
            },
          ],
        },
      ],
    });
  }),
  http.get('/tracker/' + simpleSinkTracker.id, () => {
    const linkedTrackers: {[key: number]: Tracker} = {};
    linkedTrackers[simpleOriginTracker.id] = simpleOriginTracker;
    return HttpResponse.json({
      path: simpleSinkTracker.path,
      linkedTrackers: linkedTrackers,
      regions: [
        {
          offset: 0,
          length: 12,
          content: 'Not tracked\n',
          parts: [],
        },
        {
          offset: 12,
          length: 3,
          content: 'foo',
          parts: [
            {
              trackerId: simpleOriginTracker.id,
              offset: 10,
              length: 3,
            },
          ],
        },
        {
          offset: 15,
          length: 1,
          content: ',',
          parts: [],
        },
        {
          offset: 16,
          length: 3,
          content: 'bar',
          parts: [
            {
              trackerId: simpleOriginTracker.id,
              offset: 20,
              length: 3,
            },
          ],
        },
      ],
    });
  }),
  http.get(
    '/tracker/' + simpleOriginTracker.id + '_to_' + simpleSinkTracker.id,
    () => {
      return HttpResponse.json({
        path: simpleOriginTracker.path,
        regions: [
          {
            offset: 0,
            length: 10,
            content: 'Value 1:  ',
            parts: [],
          },
          {
            offset: 10,
            length: 3,
            content: 'foo',
            parts: [
              // could be included in the response, but currently unused
              // {
              //     "trackerId": simpleSinkTracker.id,
              //     "offset": 12,
              //     "length": 3
              // }
            ],
          },
          {
            offset: 13,
            length: 7,
            content: 'Value2:',
            parts: [],
          },
          {
            offset: 20,
            length: 3,
            content: 'bar',
            parts: [
              // could be included in the response, but currently unused
              // {
              //     "trackerId": simpleSinkTracker.id,
              //     "offset": 16,
              //     "length": 3
              // }
            ],
          },
        ],
      });
    }
  ),
  http.get(
    '/tracker/' + classOriginTracker.id + '_to_' + simpleSinkTracker.id,
    () => {
      return HttpResponse.json({
        path: classOriginTracker.path,
        hasSource: true,
        linkedTrackers: {},
        regions: [
          {
            offset: 0,
            length: 12,
            content: 'Region without line\n',
            parts: [],
          },
          {
            offset: 12,
            length: 10,
            content: 'Region with line 1\n',
            // included in the response, but currently unused:
            // line: 1
            parts: [],
          },
        ],
      });
    }
  ),
  http.get('/code/' + classOriginTracker.id, () => {
    return HttpResponse.json({
      lines: [
        {
          line: 1,
          content: 'source line 1',
          parts: [
            {
              tracker: classOriginTracker,
              offset: 12,
              length: 10,
            },
          ],
        },
      ],
    });
  }),
];
