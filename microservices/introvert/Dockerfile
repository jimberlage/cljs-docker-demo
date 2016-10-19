FROM python:latest

RUN pip install flask

ENV PORT=3000

EXPOSE 3000

ADD . /opt/introvert

WORKDIR /opt/introvert

CMD python3 main.py
