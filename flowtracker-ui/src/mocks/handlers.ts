import { http, HttpResponse } from 'msw'
 
export const handlers = [
  http.get('/tree', () => {
    return HttpResponse.json({"names":["<root>"], "children":[{"names":["Category"], "children": [
      {
        "names": ["tracker1"],
        "children":[],
        "tracker":{"id":101, "path":["Category", "tracker1"], "origin":true, "sink":false, "description":"my description"}
      }
    ]}]})
  }),
]