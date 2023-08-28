FROM python:3.11

WORKDIR /deployment

COPY ./requirements.txt /deployment/requirements.txt

RUN pip install --no-cache-dir --upgrade -r /deployment/requirements.txt

ENV PYTHONPATH /deployment/src

COPY ./src /deployment/src

CMD ["uvicorn", "src.start:app", "--host", "0.0.0.0", "--port", "80"]

