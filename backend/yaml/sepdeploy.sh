#!/bin/bash
sudo kubectl create -f mongo-svc-a.yaml 
sudo kubectl create -f mongo-svc-b.yaml
sudo kubectl create -f mongo-svc-c.yaml
sudo kubectl create -f mongo-rc1.yaml
sudo kubectl create -f mongo-rc2.yaml
sudo kubectl create -f mongo-rc3.yaml
sudo kubectl create -f ocr-svc-a.yaml
sudo kubectl create -f ocr-rc1.yaml 
