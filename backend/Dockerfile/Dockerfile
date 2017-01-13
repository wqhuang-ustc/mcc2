FROM ubuntu:16.04
RUN apt-get update
RUN apt-get install -y software-properties-common
RUN add-apt-repository universe
RUN apt-get update
RUN apt-get -y install python3-pip
RUN apt-get -y install python-bottle
RUN pip3 install pytesseract
RUN pip3 install pymongo
RUN pip3 install bottle
RUN pip3 install pillow
RUN apt-get -y install tesseract-ocr
WORKDIR /src
COPY . /src/
RUN python3 configure.py
CMD ["python3", "server.py"]
#CMD python3 configure.py && python3 server.py
