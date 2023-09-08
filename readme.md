# Dev email address
words.inmemory.dev@yandex.com

# User Stories:

[done] As a user I want to enter a foreign word or sentence with its translation.

[done] As a user I want the word I have enter to be cached in app.

As a user I want in case of sentence app split it into words. 

[done] As a user I want app shows me this words or sentences on the dashboard. 

[done] As a user I do not want to see a word translation when I see this word. 

[done] As a user I want to see translation when I tap on the word. 

[done] As a user I want to mark word as completed when I read it without translation. 

[done] As a user I want to have completed words to be shadowed (less visible compare to not completed words).

[done] As a user I want to filter words by its flag "completed" or "uncompleted".

[done] As a user I want to have non default launcher icon. 

[done] As a user I want to modify translation of word or sentence.  

## Deployment steps to support automatic Chinese text word segmentation, translation, pinyin extension


Run as a plain python server:

1. $ cd src/
2. $ uvicorn start:app --host 0.0.0.0 --port 8000 --reload

(app.py is WSGI version of server which is required by some web hostings with a free quota; by default ASGI version is used which is implemented in start.py)

Run via docker: (takes ~6 GB, plus 400 MB for downloaded pytorch model; on specialized hosting it is less as they usually have many things pre-installed)

1. $ docker image build -t wim-backend .
2. $ docker run -d --name wim-backend-container -p 80:80 --net host wim-backend

(all next container launches are done via command $ docker container start wim-backend-container)

Command for test deployment: 

$ curl --header "Content-Type: application/json"   --request POST   --data '{"text" : "之后你看看了我的出版请告诉我你认为什么"}' http://127.0.0.1:8000/classify

Sample output: [["之后","你","看看","了","我","的","出版","请","告诉","我","你","认为","什么"]]

Update ```config.xml``` with endpoint ip address and backend support flag into mobile client:
```
    <string name="endpoint">http://192.168.1.19:80</string> <!-- docker based service -->
    <bool name="with_backend">true</bool>
```
