### Deploy backend

Run as a plain python server:

1. $ cd src/
2. $ uvicorn start:app --reload

(app.py is WSGI version of server which is required by some web hostings with a free quota; by default ASGI version is used which is implemented in start.py)

Run via docker:

1. $ docker image build -t wim-backend .
2. $ docker run -d --name wim-backend-container -p 80:80 wim-backend

Command for test deployment: 

$ curl --header "Content-Type: application/json"   --request POST   --data '{"text" : "之后你看看了我的出版请告诉我你认为什么"}' http://127.0.0.1:8000/classify

Sample output: [["之后","你","看看","了","我","的","出版","请","告诉","我","你","认为","什么"]]