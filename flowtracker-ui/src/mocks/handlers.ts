import {http, HttpHandler, HttpResponse} from 'msw';

export const simpleOriginTracker = {
  id: 101,
  path: ['Simple', 'origin1'],
  origin: true,
  sink: false,
  description: 'Simple origin',
};

export const simpleSinkTracker = {
  id: 102,
  path: ['Simple', 'sink1'],
  origin: false,
  sink: true,
  description: 'Simple sink',
};

export const handlers: HttpHandler[] = [
  http.get('/tree', () => {
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
                    description: 'my description',
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
    return HttpResponse.json({
      path: simpleSinkTracker.path,
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
              tracker: simpleOriginTracker,
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
              tracker: simpleOriginTracker,
              offset: 20,
              length: 3,
            },
          ],
        },
      ],
    });
  }),
  http.get(
    '/tracker/' + simpleOriginTracker.id + '/to/' + simpleSinkTracker.id,
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
              // included in the response, but currently unused
              // {
              //     "tracker": simpleSinkTracker,
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
              // included in the response, but currently unused
              // {
              //     "tracker": simpleSinkTracker,
              //     "offset": 16,
              //     "length": 3
              // }
            ],
          },
        ],
      });
    }
  ),
];
