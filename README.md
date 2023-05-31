## CSV-Json converter

Simple app that converts csv files from given location (url) and converts that into JSON format. 
Implemented with use of libraries from ZIO stack. 

Running the app: 
```
sbt run
```

API usage sample: 

Create task:
```
curl -X POST "<host>/task" -d '{ "filePath": "<csv source>"}'
```
```json
{
  "id": "61def2fd-c3f8-4638-9063-e99ea2b9322b"
}
```

List running tasks:  
```
 curl "<host>/tasks" 
```
```json
[
  {
    "taskId": {
      "id": "48ea3ac2-2bbf-4e08-849f-ac32c97435b2"
    },
    "sourceUrl": {
      "url": "https://data.wa.gov/api/views/f6w7-q2d2/rows.csv?accessType=DOWNLOAD"
    },
    "taskState": {
      "Done": {}
    }
  }
]
```

Cancel task: 
```
curl -X DELETE "<host>/task/30ee9053-ba75-4be1-a4d1-b61dee3e965f"
```
```json
{
  "taskId": {
    "id": "30ee9053-ba75-4be1-a4d1-b61dee3e965f"
  },
  "sourceUrl": {
    "url": "https://data.wa.gov/api/views/f6w7-q2d2/rows.csv?accessType=DOWNLOAD"
  },
  "taskState": {
    "Cancelled": {}
  }
}
```

Download result: 
```
curl -o "foo.json" "<host>/result/48ea3ac2-2bbf-4e08-849f-ac32c97435b2"
```

Get task state (blocking, via Web Socket): 
```
wscat -c "ws://<host>/task/61def2fd-c3f8-4638-9063-e99ea2b9322b"
```
```json
Connected (press CTRL+C to quit)
< {"taskId":{"id":"61def2fd-c3f8-4638-9063-e99ea2b9322b"},"sourceUrl":{"url":"https://data.wa.gov/api/views/f6w7-q2d2/rows.csv?accessType=DOWNLOAD"},"taskState":{"Running":{}}}}
< {"taskId":{"id":"61def2fd-c3f8-4638-9063-e99ea2b9322b"},"sourceUrl":{"url":"https://data.wa.gov/api/views/f6w7-q2d2/rows.csv?accessType=DOWNLOAD"},"taskState":{"Running":{}}}}
< {"taskId":{"id":"61def2fd-c3f8-4638-9063-e99ea2b9322b"},"sourceUrl":{"url":"https://data.wa.gov/api/views/f6w7-q2d2/rows.csv?accessType=DOWNLOAD"},"taskState":{"Running":{}}}}
< {"taskId":{"id":"61def2fd-c3f8-4638-9063-e99ea2b9322b"},"sourceUrl":{"url":"https://data.wa.gov/api/views/f6w7-q2d2/rows.csv?accessType=DOWNLOAD"},"taskState":{"Running":{}}}}
```

TODO: 
 - add tests
 - add logger
