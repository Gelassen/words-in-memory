from typing import Union

from typing import Optional, Any
from fastapi import FastAPI
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from model import ChineseTextClassifier

app = FastAPI()

model = ChineseTextClassifier()

class TextForClassification(BaseModel):
    text: str

@app.get("/")
def read_root():
    return {"Hello": "World"}

@app.post("/classify")
async def classify(payload: TextForClassification):
    result = await model.run_single_word_segmentation([payload.text])
    return get_response(True, result)

# ref. https://pypi.org/project/fastapi-queue/
def get_response(success_status: bool, result: Any) -> JSONResponse | dict:
    if success_status:
        return {"status": 200, "data": result}
    if result == -1:
        return JSONResponse(status_code=503, content="Service Temporarily Unavailable")
    else:
        return JSONResponse(status_code=500, content="Internal Server Error")
