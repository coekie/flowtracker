import { http, HttpResponse } from 'msw'
 
export const handlers = [
  http.get('/tree', () => {
    return HttpResponse.json({names:["<root>"], "children":[
      {"names":["Simple"], "children": [
        {
          "names": ["tracker1"],
          "children":[],
          "tracker":{"id":101, "path":["Simple", "tracker1"], "origin":true, "sink":false, "description":"my description"}
        },
      ]},
      // example where multiples parts of the path are squashed together in one node
      {"names":["CombinedPath"], "children": [
        {
          "names": ["one", "two", "three"],
          "children": [
            {
              "names": ["tracker1"],
              "children":[],
              "tracker":{"id":201, "path":["CombinedPath", "one", "two", "three", "tracker1"], "origin":true, "sink":false, "description":"my description"}
            },
          ]
        }
      ]},
    ]})
  }),
]