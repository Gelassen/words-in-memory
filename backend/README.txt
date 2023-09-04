### Deploy backend

Run as a plain python server:

1. $ cd src/
2. $ start:app --host 0.0.0.0 --port 8000 --reload

(app.py is WSGI version of server which is required by some web hostings with a free quota; by default ASGI version is used which is implemented in start.py)

Run via docker: (takes ~6 GB, plus 400 MB for downloaded pytorch model; on specialized hosting it is less as they usually have many things pre-installed)

1. $ docker image build -t wim-backend .
2. $ docker run -d --name wim-backend-container -p 80:80 wim-backend

Command for test deployment: 

$ curl --header "Content-Type: application/json"   --request POST   --data '{"text" : "之后你看看了我的出版请告诉我你认为什么"}' http://127.0.0.1:8000/classify

Sample output: [["之后","你","看看","了","我","的","出版","请","告诉","我","你","认为","什么"]]