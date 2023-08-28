from typing import Union

from fastapi import FastAPI
from pydantic import BaseModel
from model import ChineseTextClassifier
# from fastapi.middleware.wsgi import WSGIMiddleware
# from a2wsgi import ASGIMiddleware

app = FastAPI()
# wsgi_app = ASGIMiddleware(app)

class Item(BaseModel):
    name: str
    price: float
    is_offer: Union[bool, None] = None

class TextForClassification(BaseModel):
    text: str

@app.get("/")
def read_root():
    return {"Hello": "World"}


# @app.get("/items/{item_id}")
# def read_item(item_id: int, q: Union[str, None] = None):
#     return {"item_id": item_id, "q": q}


# @app.put("/items/{item_id}")
# def update_item(item_id: int, item: Item):
#     return {"item_name": item.name, "item_id": item_id}

# @app.get("/classify/test")
# def classify_test():
#     model = ChineseTextClassifier()
#     test = model.test()
#     return { "classified_text": test }

@app.post("/classify")
def classify(payload: TextForClassification):
    model = ChineseTextClassifier()
    result = model.run_single_word_segmentation([payload.text])
    return result

# app.mount("/", wsgi_app)

# uvicorn start:app --reload
# curl --header "Content-Type: application/json"   --request POST   --data '{"text" : "之后你看看了我的出版请告诉我你认为什么"}' http://127.0.0.1:8000/classify
# [["之后","你","看看","了","我","的","出版","请","告诉","我","你","认为","什么"]]